# CraftEngine Re-Engineering Guide & Prompts

Bu döküman, CraftEngine'in kullandığı ileri seviye teknikleri başka bir AI'a (Cursor, Claude, GPT-4) adım adım yaptırabilmen için hazırlanmış teknik bir "Blueprint" setidir.

---

## 1. Mimari Mantık (Özet)
CraftEngine'in yaptığı şey aslında bir **"Fake Registry"** ve **"Packet Remapping"** motorudur.
- Sunucu tarafında Minecraft'ı kandırıp "Bak bu yeni bir blok!" der.
- İstemci tarafında ise "Aslında o bir Note Block, sadece modeli farklı!" der.

---

## 2. AI İçin Teknik Promptlar

### Adım 1: ByteBuddy ile NMS Blok Enjeksiyonu
**AI'a Verilecek Prompt:**
> "Java 21 ve Paper API 1.21.11 kullanarak bir sistem yazmanı istiyorum. ByteBuddy kütüphanesini kullanarak `net.minecraft.world.level.block.Block` sınıfını çalışma anında (runtime) subclass yapmalısın. Amacım, yeni bir blok türü oluşturmak. Bu sınıfta `getShape` ve `getCollisionShape` metodlarını override ederek dışarıdan verdiğim `VoxelShape` verilerini döndürmeliyim. Ayrıca bu yeni blok sınıfının sunucunun dahili blok kayıt defterine (IdMapper) nasıl enjekte edileceğini göster."

### Adım 2: Netty Paket Yakalama (ID Remapping)
**AI'a Verilecek Prompt:**
> "Minecraft Paper sunucusunda Netty pipeline enjeksiyonu kullanarak giden paketleri yakalamak istiyorum. `ClientboundBlockUpdatePacket` ve `ClientboundLevelChunkWithLightPacket` paketlerini hedef almalısın. Eğer paket içindeki bir blok ID'si benim belirlediğim 'Özel ID' listesindeyse, bu ID'yi istemciye gönderilmeden önce vanilla bir ID (örneğin Note Block) ile değiştiren bir `ChannelDuplexHandler` kodu yazar mısın? Chunk paketleri içindeki PalettedContainer verisini bit-level manipülasyonla nasıl okuyup değiştirebileceğime odaklan."

### Adım 3: Modern Eşya Sistemi (1.20.5+ Components)
**AI'a Verilecek Prompt:**
> "Minecraft 1.20.5 ve sonrası için yeni Data Component API'sini kullanarak özel bir eşya sistemi kurmak istiyorum. `CustomModelData` tabanlı değil, doğrudan `EQUIPPABLE` ve `ITEM_MODEL` componentlerini manipüle eden bir yapı kurmalıyız. Özel bir Trident ve özel bir zırh seti için, bu componentleri kullanarak kaynak paketindeki (resource pack) farklı dokuları nasıl gösterebilirim? Kod örneği Paper API üzerinden olmalı."

### Adım 4: Dinamik Kayıt (Holder Pattern)
**AI'a Verilecek Prompt:**
> "Sunucu başlarken blokların ID'lerini rezerve eden ancak asıl blok objesini motor yüklendikten sonra bağlayan (Late Binding) bir 'Holder' sistemi yaz. `Holder<T>` sınıfı içermeli ve bu holder eklenti reload edildiğinde bile referansı kaybetmemeli, sadece içindeki objeyi güncellemeli."

---

## 3. Mühendislik Püf Noktaları (Wiki)

### 1. Blok Çarpışmaları (Collision)
CraftEngine, özel blokların şekillerini sunucuya `VoxelShape` olarak tanıtır. Bu sayede oyuncu o bloğun üstünde dururken titremez (desync olmaz). Bu shapes verilerini `Block.box(x1, y1, z1, x2, y2, z2)` ile tanımlar.

### 2. Note Block Manipülasyonu
Neden Note Block kullanılır? Çünkü Note Block'un 800'den fazla varyasyonu vardır (instrument, pitch, powered). CraftEngine, her bir özel blok tipini Note Block'un bir varyasyonuna eşler. İstemci (oyuncu), 3D modelini bu varyasyonlara göre görür.

### 3. Zırh ve Trident (Special Rendering)
Eski sistemde Trident'leri değiştirmek imkansızdı. CraftEngine, `thrown_item` componentini kullanarak fırlatılan Trident objesinin içindeki eşya verisini değiştirir. Zırhlarda ise `equipment_asset` kullanarak kaynak paketindeki `atlases` içinde tanımlı katmanları tetikler.

---

## 4. Kullanılacak Kütüphaneler
Eğer sıfırdan yapacaksan şu bağımlılıkları `build.gradle` dosyana eklemelisin:
- **ByteBuddy**: Bytecode manipülasyonu için.
- **Netty**: Ağ paketleri için.
- **JOML**: 3D hesaplamalar (Trident rotasyonu vb.) için.
- **Adventure API**: Zengin metinler için.
