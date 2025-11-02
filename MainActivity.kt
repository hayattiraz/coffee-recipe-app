@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.kahvetarifiuygulamasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/* ===================== MODEL ===================== */

enum class Temp { SICAK, SOGUK }
enum class Method { ESPRESSO, GRANUL }

data class RecipeVariant(
    val ingredients: List<String>,
    val steps: List<String>,
    val tips: String = ""                  // yöntem-özel ipucu (opsiyonel)
)

data class CoffeeRecipe(
    val id: String,
    val name: String,
    val temp: Temp,
    val variants: Map<Method, RecipeVariant>,
    val tips: List<String> = emptyList()   // KAHVE-ÖZEL genel ipuçları (her kahvede var)
)

/* ===================== REPOSITORY ===================== */

object CoffeeRepository {
    private fun e(ing: List<String>, steps: List<String>, tips: String = "") =
        Method.ESPRESSO to RecipeVariant(ing, steps, tips)
    private fun g(ing: List<String>, steps: List<String>, tips: String = "") =
        Method.GRANUL to RecipeVariant(ing, steps, tips)

    val recipes: List<CoffeeRecipe> = listOf(
        /* --------- SICAK: Espresso bazlı --------- */
        CoffeeRecipe(
            id = "espresso",
            name = "Espresso",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("18–20 g ince öğütüm"), listOf("25–30 sn’de ~36–40 g shot al (1:2)."), "Tat ekşiyse süreyi uzat, acıysa kısalt."),
                g(listOf("2–3 tk granül", "30–40 ml sıcak su"), listOf("Granülü az suyla yoğun çözelti yap, küçük fincanda iç."))
            ),
            tips = listOf(
                "Taze kavrum + 7–14 gün dinlenme genelde ideal.",
                "Çıkış oranını sabitle (1:2), tadı öğütümle ince ayarla."
            )
        ),
        CoffeeRecipe(
            id = "doppio",
            name = "Doppio",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("18–20 g x2 portafiltre"), listOf("25–30 sn’de ~60–80 g çift shot.")),
                g(listOf("4–5 tk granül", "60–80 ml sıcak su"), listOf("Granülü çöz ve yoğun çift içim hazırla."))
            ),
            tips = listOf("Gövde yüksek olmalı; asidite rahatsız ederse öğütümü tık kalınlaştır.")
        ),
        CoffeeRecipe(
            id = "ristretto",
            name = "Ristretto",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("18–20 g ince öğütüm"), listOf("Kısa çıkış: 15–25 g; tat yoğun ve tatlımsı.")),
                g(listOf("2–3 tk granül", "20–30 ml su"), listOf("Çok az suyla yoğun mini içim."))
            ),
            tips = listOf("Kısa çıkış karamelleşmeyi öne çıkarır; bitterlik yerine tatlı gövde.")
        ),
        CoffeeRecipe(
            id = "lungo",
            name = "Lungo",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("18–20 g ince öğütüm"), listOf("Uzun çıkış: ~80–110 g; aşırı acılıkta öğütümü kalınlaştır.")),
                g(listOf("2 tk granül", "200 ml sıcak su"), listOf("Granülü suda erit, uzun kahve olarak servis."))
            ),
            tips = listOf("Aşırı uzatmak bitterliği artırır; 80–100 g makul sınır.")
        ),
        CoffeeRecipe(
            id = "americano",
            name = "Americano",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "120–150 ml sıcak su"), listOf("Bardağa önce su, sonra espresso.")),
                g(listOf("2–3 tk granül", "180–220 ml sıcak su"), listOf("Oranı 1:10–1:15 aralığında dene."))
            ),
            tips = listOf("Önce su sonra espresso dökmek crema’yı korur.", "Filtre su tat profilini iyileştirir.")
        ),
        CoffeeRecipe(
            id = "cappuccino",
            name = "Cappuccino",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "150 ml süt"),
                    listOf("Espressoyu demle.", "Sütü yoğun köpürt.", "1/3 espresso + 1/3 süt + 1/3 köpük.")),
                g(listOf("2 tk granül", "30 ml su", "150 ml süt"),
                    listOf("Granülü erit.", "Sütü köpürt.", "Kahve üstüne süt ve köpük."))
            ),
            tips = listOf("Köpük ‘kuruya yakın’ olmalı; kaşıkta taşınabilir doku.", "Fincanı ısıtmak sıcaklık kaybını azaltır.")
        ),
        CoffeeRecipe(
            id = "latte",
            name = "Latte",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "220 ml süt"),
                    listOf("Espresso.", "Sütü 60–65°C mikroköpük yap.", "Kahvenin üstüne sütü dök.")),
                g(listOf("2 tk granül", "30 ml su", "220 ml süt"),
                    listOf("Granülü az suyla erit.", "Sütü ısıt/köpürt.", "Bardağa kahve + süt."))
            ),
            tips = listOf("Süt 60–65°C aralığında en tatlı halini verir.", "Mikroköpük doku latte art için idealdir.")
        ),
        CoffeeRecipe(
            id = "flat_white",
            name = "Flat White",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("Kısa shot/ristretto", "120–140 ml süt"),
                    listOf("Shot hazırla.", "Sütü ince mikroköpük yap.", "Düşük yükseklikten dök.")),
                g(listOf("2 tk granül", "30 ml su", "120–140 ml süt"),
                    listOf("Granülü erit.", "Sütü mikroköpük yap.", "Birleştir."))
            ),
            tips = listOf("Latte’ye göre daha az süt → daha yoğun kahve tadı.", "Ristretto gövdeyi artırır.")
        ),
        CoffeeRecipe(
            id = "macchiato",
            name = "Macchiato",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "1–2 YK süt köpüğü"), listOf("Espresso üstüne 1–2 kaşık köpük koy.")),
                g(listOf("2 tk granül", "30 ml su", "1–2 YK süt köpüğü"), listOf("Granülü erit, köpük ekle."))
            ),
            tips = listOf("Köpük miktarı tadı hızla değiştirir; azla başla.", "Küçük fincan ısı kaybını azaltır.")
        ),
        CoffeeRecipe(
            id = "cortado",
            name = "Cortado",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "30–50 ml sıcak süt (az köpük)"), listOf("Espresso.", "Az köpüklü sütle 1:1’e yakın ‘kes’.")),
                g(listOf("2 tk granül", "30 ml su", "30–50 ml sıcak süt"), listOf("Granülü erit, az süt ekle."))
            ),
            tips = listOf("Köpük minimal; pürüzsüz doku hedefle.", "1:1 oran dengeli başlangıç.")
        ),
        CoffeeRecipe(
            id = "mocha",
            name = "Mocha",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "200 ml süt", "20–25 g çikolata/kakao"),
                    listOf("Espresso + çikolatayı karıştır.", "Köpürtülmüş süt ekle.")),
                g(listOf("2 tk granül", "30 ml su", "200 ml süt", "20–25 g çikolata/kakao"),
                    listOf("Granülü erit, çikolatayla karıştır.", "Sütü ekle/köpürt."))
            ),
            tips = listOf("Kakao kullanıyorsan önce az suyla macun yap; topaklanmaz.", "Süt 60–65°C çikolata aromasını belirginleştirir.")
        ),
        CoffeeRecipe(
            id = "espresso_macchiato",
            name = "Espresso Macchiato",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "1–2 YK süt köpüğü"), listOf("Espressoyu ‘lekele’.")),
                g(listOf("2 tk granül", "30 ml su", "1–2 YK süt köpüğü"), listOf("Granülü erit, köpük ekle."))
            ),
            tips = listOf("Küçük miktar köpük aromayı yuvarlar; fazla kaçma.")
        ),
        CoffeeRecipe(
            id = "latte_macchiato",
            name = "Latte Macchiato",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "250 ml süt (köpüklü)"),
                    listOf("Bardağa süt + köpük.", "Üstten espressoyu dök (katmanlı görünüm).")),
                g(listOf("2 tk granül", "30 ml su", "250 ml süt (köpüklü)"),
                    listOf("Granülü erit.", "Süt+köpük, üstten granül kahve."))
            ),
            tips = listOf("Yavaş döküm katmanları korur.", "Sıcaklık farkı katmanı güçlendirir.")
        ),
        CoffeeRecipe(
            id = "affogato",
            name = "Affogato",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "1 top vanilyalı dondurma"), listOf("Dondurma üstüne sıcak espresso dök.")),
                g(listOf("2 tk granül", "30 ml su", "1 top vanilyalı dondurma"), listOf("Granülü erit, dondurma üstüne dök."))
            ),
            tips = listOf("Espressoyu çok bekletmeden dök; sıcak-soğuk kontrast önemli.")
        ),
        CoffeeRecipe(
            id = "con_panna",
            name = "Con Panna",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Çırpılmış krema"), listOf("Espresso üstüne krema ekle.")),
                g(listOf("2 tk granül", "30 ml su", "Krema"), listOf("Granülü erit, üstüne krema."))
            ),
            tips = listOf("Kremayı fazla şekerleme; espresso dengesi korunmalı.")
        ),
        CoffeeRecipe(
            id = "breve",
            name = "Breve",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Half-and-half (süt+krema)"), listOf("Half-and-half ısıt, espressoya ekle.")),
                g(listOf("2 tk granül", "30 ml su", "Half-and-half"), listOf("Granülü erit, ısıtılmış karışım ekle."))
            ),
            tips = listOf("Çok ağır gelirse half-and-half’i sütle incelt.")
        ),
        CoffeeRecipe(
            id = "marocchino",
            name = "Marocchino",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Kakao", "Az süt köpüğü"), listOf("Espresso.", "Kakao serp, az köpükle bitir.")),
                g(listOf("2 tk granül", "30 ml su", "Kakao", "Az köpük"), listOf("Granülü erit.", "Kakao+köpük ekle."))
            ),
            tips = listOf("Kakaoyu fincana da serpersen koku etkisi artar.")
        ),
        CoffeeRecipe(
            id = "cafe_bombon",
            name = "Café Bombón",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Kondanse süt"), listOf("Bardağa yoğun süt, üstüne espresso (katmanlı).")),
                g(listOf("2 tk granül", "30 ml su", "Kondanse süt"), listOf("Granülü erit, kondanse sütle katman yap."))
            ),
            tips = listOf("Çok tatlıdır; küçük bardak ve yavaş içim uygundur.")
        ),
        CoffeeRecipe(
            id = "irish_coffee",
            name = "Irish Coffee",
            temp = Temp.SICAK,
            variants = mapOf(
                e(listOf("1 shot espresso", "60 ml viski", "Şeker", "Krema"),
                    listOf("Espresso+viski+şeker karıştır.", "Üstüne krema ekle.")),
                g(listOf("2 tk granül", "30 ml su", "60 ml viski", "Şeker", "Krema"),
                    listOf("Granülü erit, viski+şeker karıştır.", "Krema ekle."))
            ),
            tips = listOf("Kremayı kaşığın üzerinden dökerek üstte tut.", "Viski aroması çok baskınsa miktarı düşür.")
        ),

        /* --------- SOĞUK: Espresso bazlı --------- */
        CoffeeRecipe(
            id = "iced_americano",
            name = "Iced Americano",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Buz", "Soğuk su"), listOf("Bardağa buz+su, üstüne espresso.")),
                g(listOf("2 tk granül", "30 ml su", "Buz", "Soğuk su"), listOf("Granülü erit, soğutarak buzlu suyla tamamla."))
            ),
            tips = listOf("Espressoyu hafif soğutup eklersen buz daha az erir.")
        ),
        CoffeeRecipe(
            id = "iced_latte",
            name = "Iced Latte",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("1 shot espresso", "Buz", "220 ml soğuk süt"), listOf("Buz+süt, üstüne espresso.")),
                g(listOf("2 tk granül", "30 ml su", "Buz", "220 ml soğuk süt"), listOf("Granülü erit, soğut; buz+süt üstüne ekle."))
            ),
            tips = listOf("Sütü 4–6°C kullanmak gövdeyi artırır.", "Şurubu önce sütle karıştır; dibe çökme azalır.")
        ),
        CoffeeRecipe(
            id = "iced_mocha",
            name = "Iced Mocha",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Süt", "Çikolata"), listOf("Çikolata+espresso karıştır.", "Buz+süt ekle.")),
                g(listOf("Granül", "30 ml su", "Buz", "Süt", "Çikolata"), listOf("Granül+çikolatayı karıştır.", "Buz+süt ekle."))
            ),
            tips = listOf("Çikolatayı önce az sütle aç; pürüzsüz kıvam.")
        ),
        CoffeeRecipe(
            id = "iced_cappuccino",
            name = "Iced Cappuccino",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Süt", "Soğuk süt köpüğü"), listOf("Buz+süt.", "Üstüne espresso ve köpük.")),
                g(listOf("Granül", "30 ml su", "Buz", "Süt", "Soğuk köpük"), listOf("Granülü erit, buz+süt+kıvamlı köpük ekle."))
            ),
            tips = listOf("Soğuk süt köpüğü için sütü 2–4°C’de köpürt.")
        ),
        CoffeeRecipe(
            id = "iced_macchiato",
            name = "Iced Macchiato",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Süt+buz", "Üste espresso"), listOf("Bardağa buz+süt.", "Üstten espressoyu yavaşça dök.")),
                g(listOf("Buz+süt", "Üste granül çözelti"), listOf("Granülü erit, soğut; buz+süt üzerine dök."))
            ),
            tips = listOf("Yavaş döküm katman görünümünü korur.")
        ),
        CoffeeRecipe(
            id = "cold_brew",
            name = "Cold Brew",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("60 g kalın öğütüm", "1 L soğuk su"), listOf("12–18 saat demle, filtrele.")),
                g(listOf("2–3 tk granül", "250 ml soğuk su/süt", "Buz"), listOf("Granülü az ılık suda erit, soğuk sıvı + buzla tamamla."))
            ),
            tips = listOf("1:15–1:17 oran iyi başlangıç.", "Kaba öğütüm; toz çoksa acılık artar.")
        ),
        CoffeeRecipe(
            id = "nitro_cold_brew",
            name = "Nitro Cold Brew",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Cold brew", "Nitro infüzyon"), listOf("Cold brew’ü azot ile infüze et.")),
                g(listOf("Granül baz", "Nitro (varsa)"), listOf("Granül bazını hazırla, nitro tap ile servis."))
            ),
            tips = listOf("Nitro ipeksi doku verir; buz miktarını düşük tut.")
        ),
        CoffeeRecipe(
            id = "iced_flat_white",
            name = "Iced Flat White",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Kısa shot", "Buz", "Süt"), listOf("Buz+süt, kısa shot ekle.")),
                g(listOf("Yoğun granül", "Buz", "Süt"), listOf("Granülü az suyla yoğunlaştır, buz+sütle birleştir."))
            ),
            tips = listOf("Kısa shot aromayı korur; buz erimesine dikkat.")
        ),
        CoffeeRecipe(
            id = "iced_espresso",
            name = "Iced Espresso",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("1–2 shot espresso", "Buz"), listOf("Espressoyu soğut, buz üzerinde servis.")),
                g(listOf("Yoğun granül çözelti", "Buz"), listOf("Granülü az suyla erit, buz üstünde servis."))
            ),
            tips = listOf("Sıcak espressoyu doğrudan buza dökme; tadı seyrelir.")
        ),
        CoffeeRecipe(
            id = "shakerato",
            name = "Shakerato",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Şurup"), listOf("Shaker’da buz+espresso+şurubu çalkala, süz.")),
                g(listOf("Granül çözelti", "Buz", "Şurup"), listOf("Granül bazını hazırla, buzla iyice çalkala."))
            ),
            tips = listOf("İyice çalkala; üstte ince, kremsi köpük oluşsun.")
        ),
        CoffeeRecipe(
            id = "freddo_espresso",
            name = "Freddo Espresso",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("1–2 shot espresso", "Buz", "Az şeker"), listOf("Buzla çalkala, süz.")),
                g(listOf("Yoğun granül", "Buz", "Az şeker"), listOf("Granülü az suyla çözüp buzla çalkala, süz."))
            ),
            tips = listOf("Şeker istiyorsan çalkalamadan önce ekle; daha iyi çözünür.")
        ),
        CoffeeRecipe(
            id = "freddo_cappuccino",
            name = "Freddo Cappuccino",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Freddo espresso", "Soğuk süt köpüğü"), listOf("Freddo espresso üzerine soğuk süt köpüğü ekle.")),
                g(listOf("Granül baz", "Soğuk süt köpüğü"), listOf("Granülü çalkala, üstüne soğuk süt köpüğü."))
            ),
            tips = listOf("Soğuk süt köpüğü için yağ oranı %3+ süt tercih edilebilir.")
        ),
        CoffeeRecipe(
            id = "espresso_tonic",
            name = "Espresso Tonic",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Buz", "Tonik", "Espresso"), listOf("Bardağa buz+tonik, üstten espressoyu dök.")),
                g(listOf("Buz", "Tonik", "Granül baz"), listOf("Buz+tonik, üstten granül bazını dök."))
            ),
            tips = listOf("Narenciye kabuğu ile ferahlığı artır.", "Aşırı acılıkta ‘light’ tonik dene.")
        ),
        CoffeeRecipe(
            id = "affogato_freddo",
            name = "Affogato Freddo",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Soğutulmuş espresso", "Dondurma"), listOf("Dondurma üstüne soğuk espresso.")),
                g(listOf("Soğuk granül baz", "Dondurma"), listOf("Dondurma üstüne soğuk granül kahve."))
            ),
            tips = listOf("Bardağı önceden soğutmak erimeyi yavaşlatır.")
        ),
        CoffeeRecipe(
            id = "iced_caramel_latte",
            name = "Iced Caramel Latte",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Süt", "Karamel şurubu"), listOf("Buz+süt+karamel, üstüne espresso.")),
                g(listOf("Granül baz", "Buz", "Süt", "Karamel şurubu"), listOf("Granülü erit, buz+süt+karamel ile karıştır."))
            ),
            tips = listOf("Şurubu önce sütle karıştır; dibe çökmesin.")
        ),
        CoffeeRecipe(
            id = "iced_vanilla_latte",
            name = "Iced Vanilla Latte",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Süt", "Vanilya şurubu"), listOf("Buz+süt+vanilya, üstüne espresso.")),
                g(listOf("Granül baz", "Buz", "Süt", "Vanilya şurubu"), listOf("Granülü erit, buz+süt+vanilya ile karıştır."))
            ),
            tips = listOf("Vanilya şurubu çok tatlıysa süt oranını artır.")
        ),
        CoffeeRecipe(
            id = "mocha_frappe",
            name = "Mocha Frappe",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Süt", "Çikolata", "Buz", "Blender"),
                    listOf("Tüm malzemeleri blender’da çek.")),
                g(listOf("Granül", "Süt", "Çikolata", "Buz", "Blender"),
                    listOf("Granül bazla blender’da pürüzsüz kıvam elde et."))
            ),
            tips = listOf("Buzu kademeli ekle; kıvamı daha iyi kontrol edersin.")
        ),
        CoffeeRecipe(
            id = "espresso_frappe",
            name = "Espresso Frappe",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Süt/su", "Buz", "Şeker (ops.)", "Blender"),
                    listOf("Hepsini blender’da köpüklü kıvam olana kadar karıştır.")),
                g(listOf("Granül", "Süt/su", "Buz", "Şeker (ops.)", "Blender"),
                    listOf("Granülle blender’da köpüklü kıvam elde et."))
            ),
            tips = listOf("Şeker kullanacaksan blender öncesi ekle; iyi çözünür.")
        ),
        CoffeeRecipe(
            id = "iced_breve",
            name = "Iced Breve",
            temp = Temp.SOGUK,
            variants = mapOf(
                e(listOf("Espresso", "Buz", "Half-and-half"), listOf("Buz üzerine half-and-half, üstüne espresso.")),
                g(listOf("Granül", "Buz", "Half-and-half"), listOf("Granül bazını buz+half-and-half ile birleştir."))
            ),
            tips = listOf("Ağır gelirse half-and-half’i sütle seyrelt.")
        )
    )
}

/* ===================== STATE & VM ===================== */

data class UiState(
    val tempFilter: Temp? = null,
    val list: List<CoffeeRecipe> = CoffeeRepository.recipes
)

class CoffeeViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private fun applyFilter(t: Temp?): List<CoffeeRecipe> =
        CoffeeRepository.recipes.filter { t == null || it.temp == t }

    fun setTempFilter(t: Temp?) = _state.update { s -> s.copy(tempFilter = t, list = applyFilter(t)) }

    fun get(id: String) = CoffeeRepository.recipes.find { it.id == id }
}

/* ===================== ANIMATED BACKGROUND ===================== */

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier, palette: List<Color>) {
    var index by remember { mutableStateOf(0) }
    val color by animateColorAsState(
        targetValue = palette[index],
        animationSpec = tween(durationMillis = 2600, easing = LinearEasing),
        label = "bg"
    )
    LaunchedEffect(Unit) {
        while (true) { delay(2600); index = (index + 1) % palette.size }
    }
    Box(modifier = modifier.background(color))
}

private val HotPalette = listOf(Color(0xFFFFE082), Color(0xFFFFAB91), Color(0xFFFFCC80))
private val ColdPalette = listOf(Color(0xFF81D4FA), Color(0xFFB39DDB), Color(0xFFA5D6A7))
private val DefaultPalette = listOf(Color(0xFF6EE7F9), Color(0xFFFDE68A), Color(0xFFFCA5A5))

/* ===================== ACTIVITY & NAV ===================== */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        val vm: CoffeeViewModel = viewModel()
                        ListScreen(
                            state = vm.state,
                            onTemp = vm::setTempFilter,
                            onOpen = { id -> nav.navigate("detail/$id") }
                        )
                    }
                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { bs ->
                        val vm: CoffeeViewModel = viewModel()
                        val recipe = vm.get(bs.arguments?.getString("id").orEmpty())
                        DetailScreen(recipe = recipe, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

/* ===================== UI ===================== */

@Composable
fun ListScreen(
    state: StateFlow<UiState>,
    onTemp: (Temp?) -> Unit,
    onOpen: (String) -> Unit
) {
    val s by state.collectAsState()

    val pal = when (s.tempFilter) {
        Temp.SICAK -> HotPalette
        Temp.SOGUK -> ColdPalette
        else -> DefaultPalette
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(Modifier.matchParentSize(), pal)
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopBarWithFilter(selected = s.tempFilter, onTemp = onTemp) }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(s.list, key = { it.id }) { r ->
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(r.id) }
                    ) {
                        Text(
                            text = r.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarWithFilter(selected: Temp?, onTemp: (Temp?) -> Unit) {
    TopAppBar(
        title = { Text("Kahve Tarifleri") },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected == null, { onTemp(null) }, label = { Text("Hepsi") })
                FilterChip(selected == Temp.SICAK, { onTemp(Temp.SICAK) }, label = { Text("Sıcak") })
                FilterChip(selected == Temp.SOGUK, { onTemp(Temp.SOGUK) }, label = { Text("Soğuk") })
            }
        }
    )
}

@Composable
fun DetailScreen(recipe: CoffeeRecipe?, onBack: () -> Unit) {
    val pal = when (recipe?.temp) {
        Temp.SICAK -> HotPalette
        Temp.SOGUK -> ColdPalette
        else -> DefaultPalette
    }

    var method by remember { mutableStateOf(Method.ESPRESSO) }
    var tabIndex by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(Modifier.matchParentSize(), pal)
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(recipe?.name ?: "Tarif") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Geri") } }
                )
            }
        ) { inner ->
            if (recipe == null) {
                Box(
                    Modifier.padding(inner).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Tarif bulunamadı.") }
            } else {
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Yöntem sekmeleri (Espresso / Granül)
                    TabRow(selectedTabIndex = tabIndex) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0; method = Method.ESPRESSO }, text = { Text("Espresso") })
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1; method = Method.GRANUL }, text = { Text("Granül") })
                    }

                    val v = recipe.variants[method]
                    if (v == null) {
                        Text("Bu kahve için seçilen yöntem yok.")
                    } else {
                        Section("Malzemeler") { v.ingredients.forEach { Text("• $it") } }
                        Section("Adımlar") { v.steps.forEachIndexed { i, s -> Text("${i + 1}. $s") } }
                    }

                    // KAHVE-ÖZEL GENEL İPUÇLARI (her zaman gösterilir)
                    if (recipe.tips.isNotEmpty()) {
                        Section("Püf Noktalar") {
                            recipe.tips.forEach { Text("• $it") }
                        }
                    }

                    // Yöntem-özel ipucu (varsa)
                    if (v != null && v.tips.isNotBlank()) {
                        Section("Yöntem İpucu") { Text(v.tips) }
                    }
                }
            }
        }
    }
}

/* ===================== HELPERS ===================== */

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}
