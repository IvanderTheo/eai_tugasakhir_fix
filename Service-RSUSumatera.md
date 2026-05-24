1. Admin Service (Pusat Kendali Data)
Service ini bertanggung jawab atas integritas data fundamental.
Modul Fungsional:
Master Data Pasien: CRUD (Create, Read, Update, Delete) data identitas, NIK, dan nomor rekam medis (No. RM).
User Management: Pengaturan hak akses untuk admin, dokter, dan apoteker.
Antrean & Penjadwalan: Sinkronisasi jadwal praktik dokter dengan kedatangan pasien.
Verifikasi Asuransi: Pengecekan status kepesertaan (BPJS atau asuransi swasta).
Entitas Data: Pasien, User, JadwalDokter, Poliklinik.

2. Medical Service (Layanan Klinis)
Di sini tempat data medis yang bersifat rahasia diolah.
Modul Fungsional:
Input Rekam Medis (EMR): Pencatatan riwayat penyakit, keluhan pasien, dan pemeriksaan fisik (tensi, berat badan, dll).
Diagnosis Engine: Pemilihan kode penyakit berdasarkan standar internasional (ICD-10).
E-Prescription (Resep Elektronik): Penginputan jenis obat, dosis, dan frekuensi pakai oleh dokter yang langsung terkirim ke apotek.
Tindakan Medis: Pencatatan prosedur yang dilakukan (misalnya: jahit luka, injeksi).
Entitas Data: Pemeriksaan, Diagnosa, Tindakan, Resep.

3. Pharmacy Service (Manajemen Obat & Stok)
Fokus pada logistik dan keamanan distribusi obat.
Modul Fungsional:
Inventory Control: Pelacakan stok masuk (dari supplier) dan stok keluar (ke pasien).
Validation & Dispensing: Verifikasi resep dari dokter sebelum obat disiapkan untuk mencegah salah dosis.
Stock Alert: Notifikasi otomatis jika stok obat tertentu sudah di bawah batas minimal.
Procurement (Order Obat): Pembuatan surat pesanan obat ke distributor atau vendor eksternal.
Entitas Data: Obat, Stok, Supplier, PesananObat.

4. Payment Service (Billing & Finansial)
Titik akhir yang memastikan kelangsungan ekonomi klinik.
Modul Fungsional:
Billing Aggregator: Mengambil biaya konsultasi (dari Medical Service) dan harga obat (dari Pharmacy Service) secara otomatis.
Kalkulator Diskon/Pajak: Menghitung biaya akhir setelah potongan asuransi atau penambahan PPN.
Payment Gateway Interface: Menangani berbagai metode bayar (Tunai, Debit, QRIS).
Reporting: Pembuatan laporan pendapatan harian dan bulanan.
Entitas Data: Tagihan (Invoice), Transaksi, MetodePembayaran, LaporanKeuangan.