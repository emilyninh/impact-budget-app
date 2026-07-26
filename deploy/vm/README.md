# Free deploy on an Oracle Cloud "Always Free" VM

A genuinely $0 way to host the full stack (app + Postgres + Redpanda + Redis) with HTTPS, on
Oracle Cloud's Always Free ARM VM (up to 4 OCPU / 24 GB RAM free — plenty). You run the account
/ VM steps (Oracle needs *you*); everything app-side is scripted here.

> **Cost:** Always Free resources never charge. Oracle asks for a card at signup for identity
> verification only. Stay on the **Always Free** shapes below and don't upgrade to pay-as-you-go.

## 1. Create the account + VM (Oracle web console)

1. Sign up at [oracle.com/cloud/free](https://www.oracle.com/cloud/free/).
2. **Compute → Instances → Create instance:**
   - Image: **Ubuntu 22.04**.
   - Shape: **Ampere (Arm) VM.Standard.A1.Flex**, e.g. **2 OCPU / 12 GB** (within the free 4/24).
   - Add your SSH public key (or download the generated key).
   - Create. Note the **public IP**.

## 2. Open ports 80 + 443

Two layers block traffic on Oracle Ubuntu images — do **both**:

- **VCN security list:** Networking → your VCN → default security list → add **Ingress rules**
  for `0.0.0.0/0` to TCP **80** and **443**.
- **The VM's own iptables** (Oracle's Ubuntu image blocks everything but SSH) — after SSH (step 3):
  ```bash
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
  sudo netfilter-persistent save
  ```

## 3. SSH in and install Docker

```bash
ssh ubuntu@<PUBLIC_IP>

sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu && newgrp docker   # run docker without sudo
```

## 4. Get the code + configure

```bash
git clone https://github.com/emilyninh/impact-budget-app.git
cd impact-budget-app
cp deploy/vm/.env.example deploy/vm/.env
nano deploy/vm/.env
```
Set in `deploy/vm/.env`:
- `SITE_ADDRESS` = your IP with dashes + `.sslip.io` (e.g. `152.67.1.9` → `152-67-1-9.sslip.io`).
  `sslip.io` resolves to your IP with zero DNS setup, and Caddy gets a real cert for it.
- `JWT_SECRET` = `openssl rand -base64 48`.
- (Optional) Plaid keys; set `PLAID_WEBHOOK_URL=https://<SITE_ADDRESS>/webhooks/plaid`.

## 5. Launch

```bash
docker compose -f deploy/vm/docker-compose.prod.yml up -d --build   # first build ~3-5 min
```

Watch it come up:
```bash
docker compose -f deploy/vm/docker-compose.prod.yml logs -f app     # Ctrl-C when "Started"
```

## 6. Visit it

Open **`https://<SITE_ADDRESS>`** (Caddy issues the cert on first request — give it ~15 s).
Sign in with the seeded demo account **`demo@impactbudget.app`** / **`demopass123`**, or register.

That URL is your live demo link — put it in the README and on LinkedIn.

## Operating it

```bash
# update after pushing changes
git pull && docker compose -f deploy/vm/docker-compose.prod.yml up -d --build
# stop / start
docker compose -f deploy/vm/docker-compose.prod.yml down
docker compose -f deploy/vm/docker-compose.prod.yml up -d
```

## Troubleshooting

- **Site unreachable / cert won't issue** → port 80 must be reachable for Let's Encrypt. Re-check
  both the VCN ingress rule **and** the iptables rules (step 2). `curl -I http://<IP>` from your
  laptop should connect.
- **Build killed / OOM** → use a shape with ≥ 8 GB, or add swap:
  `sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile`.
- **Only the app should be public** — this compose maps *no* host ports for Postgres/Redis/
  Redpanda; keep it that way (don't add them) so only Caddy (80/443) faces the internet.
- **`JWT_SECRET`/`SITE_ADDRESS` errors on `up`** → they're required; set them in `deploy/vm/.env`.
