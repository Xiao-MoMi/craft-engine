# Master Prompt: Bağımsız Minecraft Özel İçerik Motoru (CraftEngine Lite) Yapılandırma Rehberi

Bu döküman, CraftEngine'in çalışma mantığını teknik olarak adım adım açıklar ve bir AI'ın bu sistemi sıfırdan kurmasını sağlar.

---

## 1. ADIM: ByteBuddy ile NMS Blok Enjeksiyonu (Sunucu Tarafı)
**Mantık:** Sunucunun özel bir bloğu "gerçek" olarak kabul etmesi için Minecraft'ın dahili `Block` sınıfını çalışma anında genişletmeliyiz.

**Teknik Uygulama:**
- `net.minecraft.world.level.block.Block` sınıfını ByteBuddy ile subclass yap.
- **Override Edilecek Metodlar:**
    - `getShape` ve `getCollisionShape`: Özel `VoxelShape` döndürerek bloğun fiziksel sınırlarını belirle.
    - `updateShape`: Komşu bloklar değiştiğinde şekli güncelle (Merdivenlerin köşeleri veya çitlerin birbirine bağlanması için).
    - `rotate` ve `mirror`: Blok döndürüldüğünde (Piston veya yapı yükleme) yönü hesapla.
- **Şekilli Blok Tipleri (Shaped Blocks):**
    - **Stairs (Merdiven):** `FACING`, `HALF` ve `SHAPE` (Inner/Outer) özelliklerini (property) içermeli.
    - **Slab (Yarım Blok):** `TYPE` (TOP, BOTTOM, DOUBLE) özelliğini içermeli.
    - **Wall/Fence (Duvar/Çit):** `NORTH`, `EAST`, `SOUTH`, `WEST` bağlantı mantığını (connection logic) NMS seviyesinde simüle etmelisin.
    - **Door/Trapdoor:** `OPEN`, `POWERED`, `HALF` özelliklerini ekleyerek NMS'deki orijinal etkileşim metodlarını override et.

---

## 2. ADIM: Dahili Kayıt Defterine (Registry) Kayıt
**Mantık:** Yeni sınıfları oluşturduktan sonra sunucunun bunları tanıması gerekir.

**Teknik Uygulama:**
- `BuiltInRegistries.BLOCK` kayıt defterine eriş. Bu defter normalde dondurulmuştur (frozen).
- Reflection kullanarak `MappedRegistry` içindeki `frozen` değişkenini `false` yap.
- Yeni `Block` objeni `ResourceKey` ile kaydet.
- **ID Yönetimi:** Her özel blok durumuna (BlockState) benzersiz bir ID ata. Bu ID'leri bir `Int2ObjectMap` içinde sakla.

---

## 3. ADIM: Netty ile Paket Yakalama ve ID Dönüştürme (İstemci Tarafı)
**Mantık:** İstemci (oyuncu) senin özel blok ID'lerini bilmez. Bu yüzden paketler sunucudan çıkmadan önce onları vanilla ID'lere (Note Block gibi) çevirmelisin.

**Teknik Uygulama:**
- Oyuncu katıldığında Netty `Channel` hattına bir `ChannelDuplexHandler` enjekte et.
- **Hedef Paketler:**
    - `ClientboundBlockUpdatePacket`: Tekli blok güncellemelerinde ID'yi değiştir.
    - `ClientboundSectionBlocksUpdatePacket`: Çoklu blok güncellemelerinde ID'leri remapping yap.
    - `ClientboundLevelChunkWithLightPacket`: En zor kısmı. Paketin içindeki `PalettedContainer` verisini bit bazında (bit-level) oku. Sunucudaki "Özel ID"yi bul ve istemcinin Resource Pack ile görebileceği "Vanilla ID"ye çevir.
- **Visual Mapping:** Özel blokları genellikle `Note Block` varyasyonlarına (instrument, pitch) eşle. Çünkü Note Block'un 800+ görsel varyasyonu vardır.

---

## 4. ADIM: Modern Eşya Sistemi (1.20.5+ Components)
**Mantık:** Artık NBT yerine Data Components kullanılıyor.

**Teknik Uygulama:**
- **Zırhlar:** `EQUIPPABLE` componentini kullanarak özel doku katmanlarını (texture layers) tanımla.
- **Trident:** 1.21.4+ sürümlerinde gelen `item_model` componentini manipüle et. Fırlatılan Trident'in (Entity) görselini `thrown_item` componenti üzerinden kendi modelinle değiştir.
- **CustomModelData:** Basit eşyalar için bu değeri otomatik ata ve `s2c` (server to client) paketlerinde bu verinin paketlendiğinden emin ol.

---

## 5. ADIM: Kaynak Paketi (Resource Pack) Otomasyonu
**Mantık:** Oyuncuların bu içerikleri görmesi için gerekli JSON modelleri otomatik üretilmeli.

**Teknik Uygulama:**
- Kayıtlı her blok tipi için bir `blockstates/*.json` ve `models/block/*.json` üret.
- Note Block'un `instrument` ve `note` özelliklerini senin blok modellerine yönlendiren bir `override` tablosu oluştur.
- Tüm bu dosyaları zipleyerek oyuncuya sunucuya girişte gönder.

---

**AI'a Verilecek Komut Örneği:**
> "Yukarıdaki teknik dökümantasyonu baz alarak; Java 21, Paper API 1.21.11 ve ByteBuddy kullanarak, Minecraft sunucusunda çalışan, Merdiven ve Trident desteği olan, Netty üzerinden paket remapping yapan modüler bir özel içerik sistemi iskeleti oluştur."
