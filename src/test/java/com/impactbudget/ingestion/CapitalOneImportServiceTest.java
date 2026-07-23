package com.impactbudget.ingestion;

import com.impactbudget.common.TransactionIngested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapitalOneImportServiceTest {

    @Mock
    PlaidItemRepository itemRepository;
    @Mock
    BankTransactionRepository txnRepository;
    @Mock
    TransactionEventPublisher eventPublisher;

    private static final String CSV = """
            Transaction Date,Posted Date,Card No.,Description,Category,Debit,Credit
            2026-07-22,2026-07-22,3324,TST* DOS TOROS,Dining,5.60,
            2026-07-16,2026-07-16,8803,CAPITAL ONE AUTOPAY PYMT,Payment/Credit,,6128.69
            2026-07-12,2026-07-13,8803,CVS/PHARMACY,Health Care,34.99,
            """;

    @Test
    void importsDebitRowsSkipsCreditsAndPassesCategoryHint() throws Exception {
        when(itemRepository.findByPlaidItemId(anyString())).thenReturn(Optional.empty());
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(txnRepository.findByPlaidTransactionId(anyString())).thenReturn(Optional.empty());

        CapitalOneImportService service =
                new CapitalOneImportService(itemRepository, txnRepository, eventPublisher);
        int imported = service.importCsv("user-1",
                new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8)));

        // Two debit rows imported; the payment (Credit) row skipped.
        assertThat(imported).isEqualTo(2);
        verify(txnRepository, org.mockito.Mockito.times(2)).save(any(BankTransaction.class));

        ArgumentCaptor<TransactionIngested> events = ArgumentCaptor.forClass(TransactionIngested.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishIngested(events.capture());
        List<TransactionIngested> published = events.getAllValues();
        assertThat(published).extracting(TransactionIngested::merchantRaw)
                .containsExactly("TST* DOS TOROS", "CVS/PHARMACY");
        assertThat(published).extracting(TransactionIngested::sourceCategory)
                .containsExactly("Dining", "Health Care");
        assertThat(published.get(0).amount()).isEqualByComparingTo("5.60");
        assertThat(published.get(0).userId()).isEqualTo("user-1");
    }
}
