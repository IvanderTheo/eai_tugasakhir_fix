package com.example.paymentservice.messaging;

import com.example.paymentservice.config.KafkaConfig;
import com.example.paymentservice.entity.Tagihan;
import com.example.paymentservice.service.TagihanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SagaPaymentListener {

    @Autowired
    private TagihanService tagihanService;

    @KafkaListener(topics = KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, groupId = "payment-saga-group")
    public void listenPrescriptionReceived(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received prescription-received event: {}", event);
        try {
            String status = (String) event.get("status");
            if ("STOCK_RESERVED".equals(status)) {
                Long prescriptionId = Long.valueOf(event.get("prescriptionId").toString());
                Long patientId = Long.valueOf(event.get("pasienId").toString());
                Double hargaObat = Double.valueOf(event.get("hargaObat").toString());
                String namaObat = (String) event.get("namaObat");
                Integer jumlah = Integer.valueOf(event.get("jumlah").toString());

                log.info("Stock reserved for prescription: {}. Creating invoice...", prescriptionId);
                
                Tagihan tagihan = new Tagihan();
                tagihan.setPasienId(patientId);
                tagihan.setResepId(prescriptionId);
                tagihan.setNamaObat(namaObat);
                tagihan.setJumlahObat(jumlah);
                tagihan.setBiayaKonsultasi(50000.0); // Standard consultation fee
                tagihan.setHargaObat(hargaObat);
                tagihan.setDiskonAsuransi(0.0);
                tagihan.setStatus("PENDING");

                tagihanService.createTagihan(tagihan);
            }
        } catch (Exception e) {
            log.error("Error processing prescription-received event", e);
        } finally {
            ack.acknowledge();
        }
    }
}
