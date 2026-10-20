# Pravila rada na projektu AI Home Designer

Ovaj fajl čita svaki AI asistent koji radi na projektu, pre bilo kakve izmene.

## Cilj

Android aplikacija (Kotlin, Jetpack Compose, Room) za planiranje stana ili kuće. Najvažnija je geometrijska tačnost.

## Pravila tačnosti

- Model nekretnine je izvor istine za mere i geometriju. Slike koje generiše AI su samo vizuelizacija.
- Mere se u modelu čuvaju u metrima.
- Razlikuj izmerene, potvrđene, uvezene, procenjene i generisane podatke (`DimensionSource`). Procene nikada ne prikazuj kao proverene mere.
- Ne tvrdi da raspored ili mera "stoji" ako provera nije zaista izvršena. Poruke u aplikaciji navode šta je tačno provereno.
- Ne tvrdi da gradnja ili testovi prolaze bez dokaza iz stvarnog izvršavanja.
- Ne izmišljaj vrednosti: polje koje korisnik nije uneo ostaje prazno (null), a ne podrazumevana mera.

## Način rada

- Radi jedan zadatak odjednom, u malim isporukama: zip sa 2 do 4 fajla, jedan commit, jedno građenje. Korisnik ima ograničen broj poruka, pa i poruke treba da budu kratke.
- Ne dodaj nove biblioteke, servise ili AI integracije bez dogovora. Proveri kompatibilnost verzija.
- Ne ubacuj privatne ključeve u kod.
- Ne prikazuj dugmad koja ništa ne rade i ne prikazuj lažne podatke.
- Korisnik je početnik i radi samo sa telefona: daj tačne putanje fajlova. Fajlovi se šalju kao cele datoteke, ne kao delovi.
- Interfejs je na srpskom, jednostavnim jezikom. Tekstovi idu u `res/values/strings*.xml`.

## Tok rada (bez računara)

1. Asistent pravi zip sa kompletnim fajlovima, a korisnik ga postavlja u koren repoa.
2. Automat `unzip.yml` raspakuje zip i commituje.
3. `build-apk.yml` pokreće testove i gradi APK (pokreće se ručno posle raspakivanja).
4. Fajlovi u `.github/workflows` ne idu u zip, već se lepe ručno.

Trenutno se radi na grani `main`.

## Nastavak u novom razgovoru

Korisnik ponekad nastavlja u novom razgovoru kad dostigne ograničenje, pa repo može da bude ispred starijih zipova. Zato:

1. Repo (zip koji korisnik otpremi) je izvor istine. Zipove iz starijih poruka ne uzimaj kao stanje.
2. Pročitaj ovaj fajl i fajl koji menjaš, pa pravi izmenu od te verzije. Ne prepisuj fajl koji je u međuvremenu menjan, već ga uporedi.
3. Nemoj slati fajl koji već postoji u repou u drugačijem obliku bez razloga.

## Struktura koda (`app/src/main/java/com/zoran/aihomedesigner`)

- `domain/`: čista logika bez Androida (lako se testira): `Project`, `Room`, `Opening`, `Furniture`, provere unosa (`*Validator`), geometrija (`RoomGeometry`, `SharedWalls`, `PlanValidator`, `WallSnapping`, `DoorSwingGeometry`, `FurnitureGeometry`, `FurnitureLayoutValidator`, `FurniturePlacement`) i 3D (`Scene3D.kt`: `SceneBuilder`, `Camera3D.kt`: `Projection`).
- `data/`: Room baza (`AppDatabase`, `*Entity`, `*Dao`, `*Mapper`, `*Repository`).
- `ui/`: ekrani i `ProjectsViewModel`. Navigacija je u `MainActivity.kt` (enum `Screen`, bez biblioteke za navigaciju).
- Koordinatni sistem: `docs/koordinatni-sistem.md`. Nameštaj se meri od gornjeg levog ugla sobe, a sobe od početka plana.

## Baza

- Trenutna verzija je 5. Migracije 1 do 5 su u `AppDatabase.kt`.
- Svaka izmena šeme podiže verziju i dodaje migraciju koja ne briše podatke. SQL u migraciji mora tačno da odgovara onome što Room očekuje za entitet.
- Nikada `fallbackToDestructiveMigration`. Migraciju proverava samo telefon (ne automatski test), pa se menja šema uvek u posebnom malom zipu.
- Ako Room javi "cannot verify the data integrity", baza ne odgovara kodu. Prvo utvrdi uzrok, a brisanje podataka je krajnja mera.

## Dijagnostika

`CrashReporter` pamti poslednju grešku, a `CrashScreen` je prikazuje pri sledećem otvaranju da korisnik može da je kopira.

## Šta se proverava automatski (jedinični testovi)

Mere i unos, provere plana, vrata, nameštaj, geometrija, 3D projekcija i pretvaranje podataka za bazu. Ekrani i gestovi se proveravaju ručno na telefonu.

## Redosled razvoja

1. Osnova aplikacije i navigacija (gotovo).
2. Projekti i trajni model podataka (gotovo).
3. Ručno pravljenje soba i unos mera (gotovo).
4. Izmenljiv 2D plan (gotovo).
5. Geometrijska provera i povezane sobe, vrata i prozori (gotovo).
6. Nameštaj i provera sudara (gotovo).
7. Interaktivni 3D prikaz (gotovo, jednostavan).
8. Uvoz plana i kalibracija razmere.
9. AI vizuelizacija (opciono, bez ključeva u aplikaciji).
10. Skeniranje i napredne funkcije.
