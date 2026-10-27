# AI Home Designer

Android aplikacija za planiranje stana ili kuće: projekti, sobe sa tačnim merama, 2D plan, vrata i prozori, nameštaj i 3D prikaz.

Najvažniji zahtev je tačnost mera. Model nekretnine je izvor istine, a slike koje generiše AI nikada ne smeju da menjaju mere.

## Šta radi (verzija 0.1)

- **Projekti** (stan ili kuća) sa trajnim čuvanjem na telefonu.
- **Sobe**: pravougaone, sa unutrašnjom dužinom i širinom, visinom zida i debljinom zida (opciono). Mere su u metrima, a svaka ima oznaku izvora (npr. "upisao korisnik").
- **2D plan**: sobe u razmeri, prevlačenje prstom i strelicama, zumiranje, strane sveta (sever je gore na planu), zidovi između susednih soba i dugme za priljubljivanje sobe uz susednu.
- **Spratovi**: prizemlje, spratovi i podrumi. Svaki sprat ima svoj plan, a ime sobe mora da bude jedinstveno samo na istom spratu. Sobu je moguće prebaciti na drugi sprat (mere, vrata, prozori i nameštaj idu sa njom, a ona dobija novo mesto na planu tog sprata).
- **Zaključavanje i lepljenje soba** na planu: zaključana soba se ne pomera slučajno, a zalepljene sobe istog sprata se pomeraju zajedno.
- **Stepenice**: prave stepenice između dva sprata (unos mera, smera penjanja i sprata, provera unosa i savet o visini i dubini stepenika). Crtaju se na planu, u 3D prikazu i u izvozu plana. Visina uspona i broj stepenika su procena iz visine soba, a ne izmerena vrednost.
- **AI ilustracija** (opciono): iz plana se pravi tekstualni opis koji se šalje Google Gemini servisu za slike, uz ključ koji korisnik sam unese u aplikaciji. Slika je samo ilustracija, ne čuva se i ne menja mere. Aplikacija ima pravo na internet samo zbog ovog ekrana.
- **Provera plana**: preklapanje soba i debljina zida između susednih soba.
- **Vrata i prozori** na zidovima soba, sa položajem, merama, smerom otvaranja i stranom šarki. Luk otvaranja vrata se crta na planu.
- **Nameštaj** sa pravim merama i okretanjem, prevlačenje na planu. Provera: izlazi iz sobe, previsok je, preklapanje, blokira vrata. Uz to preporuke o uskim prolazima (manje od 60 cm između predmeta ili do zida), koje nisu greške.
- **3D prikaz** cele nekretnine sa svim spratovima jedan iznad drugog, okretanje i zumiranje prstom. Crta se iz istih podataka kao plan.
- **Uvoz plana sa slike**: slika iz galerije kao podloga ispod plana. Razmera se određuje kalibracijom (dve tačke i poznato rastojanje) i vodi se kao "uvezeno i kalibrisano", a ne kao izmereno. Sliku je moguće pomerati.
- **Izvoz plana** izabranog sprata u sliku (PNG) ili PDF i deljenje, sa lenjirom razmere i napomenom da raspored soba nije izmeren.

Svaka provera tvrdi samo ono što stvarno proverava, a aplikacija to ispisuje.

## Šta još ne radi

Uvoz svih strana PDF-a (uvozi se samo prva), automatsko prepoznavanje zidova sa slike, skeniranje prostorije, druge vrste stepenica osim pravih, sobe koje nisu pravougaone, više jezika osim srpskog.

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

- Sobe su pravougaone, a stepenice su samo prave.
- Ako upis u bazu ne uspe (na primer, pun telefon), poruka se prikazuje za brisanja, pomeranja i forme (soba, vrata, nameštaj, stepenice); forma se tada zatvori, a uneti podaci se gube. Novi projekat i kalibracija slike još nemaju tu poruku.
- Prikazi (plan, 3D) i gestovi se proveravaju ručno na telefonu. Automatski se testira logika (mere, provere, geometrija, projekcija).
- 3D prikaz je jednostavan: bez tekstura i senki. Kvadri se crtaju od daljih ka bližim, što je pouzdano za sobe i nameštaj, ali nije potpuno skrivanje površina, pa su retki složeni slučajevi mogući.
- Položaj soba na planu je raspored koji bira korisnik i nije izmeren.
