# Pravila rada na projektu AI Home Designer

Ovaj fajl čita svaki AI asistent koji radi na projektu, pre bilo kakve izmene.

## Cilj

Android aplikacija (Kotlin, Jetpack Compose, Room) za planiranje stana ili kuće. Najvažnija je geometrijska tačnost.

## Pravila tačnosti

- Model nekretnine je izvor istine za mere i geometriju. Slike koje generiše AI su samo vizuelizacija.
- Mere se u modelu čuvaju u metrima.
- Razlikuj izmerene, potvrđene, uvezene, procenjene i generisane podatke. Procene nikada ne prikazuj kao proverene mere.
- Ne tvrdi da raspored ili mera "stoji" ako provera nije zaista izvršena.
- Ne tvrdi da gradnja ili testovi prolaze bez dokaza iz stvarnog izvršavanja.

## Način rada

- Radi jedan zadatak odjednom, u malim isporukama (mali zip, jedan commit, jedno građenje).
- Ne dodaji nove biblioteke, servise ili AI integracije bez dogovora. Proveri kompatibilnost verzija.
- Ne ubacuj privatne ključeve u kod.
- Ne prikazuj dugmad koja ništa ne rade i ne prikazuj lažne podatke.
- Korisnik je početnik i radi samo sa telefona: daj tačne putanje fajlova i kompletan tekst za lepljenje. Fajlovi se šalju kao cele datoteke, ne kao delovi.
- Interfejs je na srpskom, jednostavnim jezikom. Tekstovi idu u `strings.xml`.

## Tok rada (bez računara)

1. Asistent pravi zip sa kompletnim fajlovima, a korisnik ga postavlja u koren repoa.
2. Automat `unzip.yml` raspakuje zip i commituje.
3. `build-apk.yml` pokreće testove i gradi APK (pokreće se ručno posle raspakivanja).
4. Fajlovi u `.github/workflows` ne idu u zip, već se lepe ručno.

Trenutno se radi na grani `main`.

## Redosled razvoja

1. Osnova aplikacije i navigacija (gotovo).
2. Projekti i trajni model podataka (gotovo).
3. Ručno pravljenje soba i unos mera.
4. Izmenljiv 2D plan.
5. Geometrijska provera i povezane sobe.
6. Nameštaj i provera sudara.
7. Interaktivni 3D prikaz.
8. Uvoz plana i kalibracija.
9. AI vizuelizacija.
10. Skeniranje i napredne funkcije.
