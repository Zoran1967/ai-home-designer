# AI Home Designer

Android aplikacija za planiranje stana ili kuće: projekti, sobe, tačne mere, a kasnije 2D plan, 3D prikaz i AI vizuelizacije.

Najvažniji zahtev je tačnost mera. Model nekretnine je izvor istine, a slike koje generiše AI nikada ne smeju da menjaju mere.

## Šta radi trenutno (verzija 0.1)

- Lista projekata (stan ili kuća).
- Pravljenje projekta sa proverom imena (ne sme biti prazno, najviše 60 znakova, ne sme već postojati).
- Brisanje projekta uz potvrdu.
- Trajno čuvanje na telefonu (Room baza): projekti ostaju posle zatvaranja aplikacije.

## Šta još ne radi

Sobe i mere, 2D plan, provera nameštaja, 3D prikaz, uvoz plana, skeniranje i AI vizuelizacije. Ništa od toga nije ni prikazano u aplikaciji kao da radi.

## Kako se gradi i pokreće (bez računara)

Građenje radi GitHub Actions u oblaku.

1. U repou otvori **Actions**, pa **Napravi APK**, pa **Run workflow**.
2. Kad se završi zelenom kvačicom, otvori taj pokušaj i na dnu u delu **Artifacts** preuzmi `ai-home-designer-debug-apk`.
3. Raspakuj preuzeti zip, pa instaliraj `app-debug.apk` na telefon (Android traži dozvolu za instalaciju van Play prodavnice).

Isti tok rada pre građenja pokreće i testove (`gradle testDebugUnitTest`). Ako neki test padne, građenje se zaustavlja.

## Kako se dodaje novi kod

Kod stiže kao zip arhiva. Postavi je u koren repoa preko **Add file**, pa **Upload files**. Automat **Raspakuj zip i commituj** raspakuje fajlove, upiše ih u repo i obriše zip. Fajlovi u `.github/workflows` ne mogu se menjati ovim putem i lepe se ručno.

## Građenje na računaru (kasnije, ako zatreba)

Potrebni su Android Studio, JDK 17 i Gradle 8.7. Projekat koristi Kotlin 2.0.20, Android Gradle Plugin 8.5.2, compileSdk 34, minSdk 26, Jetpack Compose i Room 2.6.1.

## Struktura koda

- `domain/` poslovna pravila: `Project`, `PropertyType`, `ProjectValidator` (bez Androida, lako se testira).
- `data/` baza: `AppDatabase`, `ProjectDao`, `ProjectEntity`, `ProjectRepository`.
- `ui/` ekrani i `ProjectsViewModel`.

## Merne jedinice

Sve arhitektonske mere u modelu čuvaju se u metrima. Pretvaranje u centimetre ili milimetre radi se samo za prikaz.

## Poznata ograničenja

- Ako upis u bazu ne uspe (na primer, pun telefon), aplikacija još ne prikazuje posebnu poruku.
- Nema migracija baze jer je to prva verzija šeme (`version = 1`). Opis šeme se izvozi pri građenju, ali se još ne čuva u repou.
- Testovi pokrivaju proveru imena i pretvaranje podataka. Ekrani i baza nisu automatski testirani, već ručno na telefonu.
