# Ultra-Master Prompt: CraftEngine Bağımsız Mühendislik Rehberi

Bu döküman, CraftEngine'in desteklediği **TÜM** blok türlerini ve ileri seviye teknikleri başka bir AI'ın hatasız yapabilmesi için hazırlanmış en kapsamlı rehberdir.

---

## **AI İÇİN PROJE GİRİŞİ VE TEKNİK VİZYON**
"Senden, Minecraft (Paper 1.21.11) için profesyonel bir özel içerik motoru yazmanı istiyorum. Bu motor, Minecraft'ın dahili sınıflarına (NMS) sızarak (injection) vanilladan farksız çalışan özel bloklar ve eşyalar eklemelidir. Projede **Java 21**, **ByteBuddy**, **Netty** ve **JOML** kütüphanelerini kullanmalısın."

---

## **1. BÖLÜM: NMS BLOK ENJEKSİYONU (BYTEBUDDY)**
"Sunucuda her bir özel blok türü için `net.minecraft.world.level.block.Block` sınıfını subclass yap. Sadece `getShape` değil, aşağıdaki tüm varyasyonların NMS mantığını override etmelisin:"

### **Desteklenecek Blok Türleri ve Mantıkları:**
1.  **Stairs (Merdiven):** `FACING`, `HALF` ve `SHAPE` (Inner/Outer) özelliklerini içermeli. `updateShape` metodunda komşu merdivenleri kontrol ederek otomatik köşe yapma mantığını kur.
2.  **Slab (Yarım Blok):** `TYPE` (TOP, BOTTOM, DOUBLE) özelliklerini içermeli. `getShape` buna göre 0-8 veya 8-16 box döndürmeli.
3.  **Pressure Plate (Basınç Plakası):** `POWERED` özelliği. `entityInside` metodunu override ederek bloğun üstündeki varlıkları algıla ve redstone sinyali üret.
4.  **Button (Buton):** `FACE` (Floor, Wall, Ceiling), `FACING` ve `POWERED`. Sağ tıklandığında `POWERED` durumunu değiştirip belirli bir tick sonra geri kapatan zamanlayıcıyı ekle.
5.  **Door & Trapdoor:** `OPEN`, `HALF`, `HINGE`. Kapıların alt ve üst yarılarının senkronize çalışması için `onPlace` ve `playerWillDestroy` metodlarını birbirine bağla.
6.  **Fence & Wall:** `NORTH`, `EAST`, `SOUTH`, `WEST` bağlantı özelliklerini (up, low, tall) NMS'deki bağlantı mantığıyla (neighbor checking) simüle et.
7.  **Leaves (Yaprak):** `PERSISTENT` ve `DISTANCE` özellikleri. Vanilla yaprakları gibi kaybolma (decay) mantığını ekle.
8.  **Falling Block (Düşen Blok):** `Fallable` interface'ini implement et. Altındaki blok kırıldığında `FallingBlockEntity` oluştur.
9.  **Crops (Ekin):** `AGE` özelliği. `randomTick` metodunda büyüme şansını hesapla ve kemik tozu (bonemeal) desteği ekle.
10. **Lamp & Toggleable:** `LIT` ve `POWERED` özellikleri. Işık seviyesini `lightEmission` üzerinden dinamik yönet.

---

## **2. BÖLÜM: NETTY PACKET REMAPPING (KRİTİK)**
"İstemciyi kandırmak için en gelişmiş packet remapping motorunu kurmalısın:"
*   **Packet Interception:** `ChannelDuplexHandler` ile `ClientboundLevelChunkWithLightPacket` içindeki `PalettedContainer` verisini bit bazında manipüle et.
*   **Note Block & Tripwire Eşlemesi:** Her özel blok durumunu (BlockState), Note Block'un 800 varyasyonundan birine veya Tripwire/Mushroom varyasyonlarına eşle.
*   **Sound System:** Özel blokların kırma ve yürüme seslerini `ClientboundSoundPacket` yakalayarak kendi ses dosyalarınla (resource pack) değiştir.

---

## **3. BÖLÜM: MODERN EŞYA VE VARLIK SİSTEMİ**
*   **Data Component API:** `CustomModelData` yerine `ITEM_MODEL` (1.21.4+) ve `EQUIPPABLE` (Armor) kullan.
*   **Custom Trident:** Fırlatılan Trident entity'sinin `thrown_item` bilgisini paket seviyesinde yakalayıp özel modelinle değiştir.
*   **Armor Layers:** Zırhların üstünde özel dokular göstermek için `EQUIPMENT_ASSET` sistemini entegre et.

---

## **4. BÖLÜM: OTOMATİK KAYNAK PAKETİ ÜRETİCİSİ**
*   Motor, kayıt edilen tüm blokları ve durumlarını okuyup otomatik olarak `assets/minecraft/blockstates/note_block.json` dosyasını oluşturmalı.
*   Tüm `instrument` ve `note` kombinasyonlarını senin özel `.json` modellerine yönlendirmeli.

---

## **AI'A SON KOMUT:**
"Şimdi, yukarıdaki mimariyi kullanarak; modüler bir `BlockInjector`, `PacketRemapper` ve `ContentRegistry` içeren, içinde Merdiven, Basınç Plakası ve Trident desteği olan eksiksiz bir plugin projesi oluştur. Kodun içinde NMS (net.minecraft) paketlerini doğrudan kullan ve versiyon uyumluluğu için esnek bir yapı kur."
