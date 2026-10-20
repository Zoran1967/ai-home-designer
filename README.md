# AI Home Designer

Android aplikacija za planiranje stana ili kuće: projekti, sobe sa tačnim merama, 2D plan, vrata i prozori, nameštaj i 3D prikaz.

Najvažniji zahtev je tačnost mera. Model nekretnine je izvor istine, a slike koje generiše AI (kad se jednog dana dodaju) nikada ne smeju da menjaju mere.

## Šta radi (verzija 0.1)

- **Projekti** (stan ili kuća) sa trajnim čuvanjem na telefonu.
- **Sobe**: pravougaone, sa unutrašnjom dužinom i širinom, visinom zida i debljinom zida (opciono). Mere su u metrima, a svaka ima oznaku izvora (npr. "upisao korisnik").
- **2D plan**: sobe u razmeri, prevlačenje prstom i strelicama, zumiranje, strane sveta (sever je gore na planu), zidovi između susednih soba i dugme za priljubljivanje sobe uz susednu.
- **Provera plana**: preklapanje soba i debljina zida između susednih soba.
- **Vrata i prozori** na zidovima soba, sa položajem, merama, smerom otvaranja i stranom šarki. Luk otvaranja vrata se crta na planu.
- **Nameštaj** sa pravim merama i okretanjem, prevlačenje na planu. Provera: izlazi iz sobe, previsok je, preklapanje, blokira vrata.
- **3D prikaz** cele nekretnine, okretanje i zumiranje prstom. Crta se iz istih podataka kao plan.

Svaka provera tvrdi samo ono što stvarno proverava, a aplikacija to ispisuje.

## Šta još ne radi

Uvoz plana sa slike ili PDF-a i kalibracija razmere, skeniranje prostorije, AI vizuelizacija, spratovi, sobe koje nisu pravougaone, širina prolaza između predmeta, izvoz plana, više jezika osim srpskog.

## Kako se gradi i pokreće (bez računara)

Građenje radi GitHub Actions u oblaku.

1. U repou otvori **Actions**, pa **Napravi APK**, pa **Run workflow**.
2. Kad se završi zelenom kvačicom, otvori taj pokušaj i na dnu u delu **Artifacts** preuzmi `ai-home-designer-debug-apk`.
3. Raspakuj preuzeti zip, pa instaliraj `app-debug.apk` na telefon.

Isti tok rada pre građenja pokreće testove (`gradle testDebugUnitTest`). Ako neki test padne, građenje se zaustavlja. Debug APK se potpisuje stalnim ključem iz `app/debug.keystore`, pa se nova verzija instalira preko stare bez brisanja podataka. Taj ključ je javan i ne sme se koristiti za objavu u Play prodavnici.

## Kako se dodaje novi kod

Kod stiže kao zip arhiva sa celim fajlovima. Postavi je u koren repoa preko **Add file**, pa **Upload files**. Automat **Raspakuj zip i commituj** raspakuje fajlove i commituje ih. Fajlovi u `.github/workflows` ne mogu se menjati ovim putem i lepe se ručno.

## Ako se aplikacija zatvori pri otvaranju

Aplikacija pamti tehnički opis greške i prikazuje ga na ekranu pri sledećem otvaranju. Dodirni **Kopiraj** i pošalji tekst programeru. Podatke ne briši pre nego što se vidi uzrok.

## Merne jedinice i koordinate

Sve mere u modelu su u metrima. Opis koordinatnog sistema je u `docs/koordinatni-sistem.md`.

## Poznata ograničenja

- Pravougaone sobe, bez spratova.
- Ako upis u bazu ne uspe (na primer, pun telefon), aplikacija još ne prikazuje posebnu poruku.
- Prikazi (plan, 3D) i gestovi se proveravaju ručno na telefonu. Automatski se testira logika (mere, provere, geometrija, projekcija).
- 3D prikaz je jednostavan: bez tekstura i senki, a dalje stranice se ponekad pogrešno slože sa bližim.
- Položaj soba na planu je raspored koji bira korisnik i nije izmeren.
