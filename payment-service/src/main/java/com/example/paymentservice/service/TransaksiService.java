package com.example.paymentservice.service;

import com.example.paymentservice.config.KafkaConfig;
import com.example.paymentservice.entity.Tagihan;
import com.example.paymentservice.entity.Transaksi;
import com.example.paymentservice.repository.TagihanRepository;
import com.example.paymentservice.repository.TransaksiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TransaksiService {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private TagihanRepository tagihanRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Transaksi processPayment(Transaksi transaksi) {
        Optional<Tagihan> optionalTagihan = tagihanRepository.findById(transaksi.getTagihanId());
        if (optionalTagihan.isEmpty()) {
            throw new RuntimeException("Invoice not found");
        }

        Tagihan tagihan = optionalTagihan.get();
        
        // Validate payment amount
        if (transaksi.getJumlahBayar() < tagihan.getTotalBayar()) {
            throw new RuntimeException("Payment amount is insufficient");
        }

        transaksi.setStatusPembayaran("COMPLETED");
        Transaksi saved = transaksiRepository.save(transaksi);

        // Update invoice status
        tagihan.setStatus("PAID");
        tagihanRepository.save(tagihan);

        // Publish payment processed event
        Map<String, Object> event = new HashMap<>();
        event.put("id", saved.getId());
        event.put("tagihanId", saved.getTagihanId());
        event.put("resepId", tagihan.getResepId());
        event.put("pasienId", tagihan.getPasienId());
        event.put("namaObat", tagihan.getNamaObat());
        event.put("jumlah", tagihan.getJumlahObat());
        event.put("status", "SUCCESS");
        event.put("jumlahBayar", saved.getJumlahBayar());
        event.put("metodePembayaran", saved.getMetodePembayaran());
        event.put("timestamp", System.currentTimeMillis());

        String eventKey = tagihan.getResepId() != null ? tagihan.getResepId().toString() : saved.getId().toString();
        kafkaTemplate.send(KafkaConfig.PAYMENT_PROCESSED_TOPIC, eventKey, event);

        log.info("Payment processed with transaction ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Transaksi getTransaksiById(Long id) {
        Optional<Transaksi> optional = transaksiRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Transaction not found");
        }
        return optional.get();
    }

    @Transactional(readOnly = true)
    public List<Transaksi> getTransaksiByTagihanId(Long tagihanId) {
        return transaksiRepository.findByTagihanId(tagihanId);
    }
}

