# Koordinatni sistem projekta

- Jedinica: metar. Sve arhitektonske mere i koordinate u modelu su u metrima.
- Početak (0, 0): proizvoljna tačka plana. Nije vezana za zgradu.
- Osa x ide udesno, osa y ide nadole (kao na ekranu telefona). Na planu je gore sever, desno istok.
- Soba je pravougaonik čiji je položaj gornji levi ugao unutrašnjosti sobe (xM, yM).
  Dužina (lengthM) ide duž ose x, a širina (widthM) duž ose y.
  Soba zauzima područje od (xM, yM) do (xM + lengthM, yM + widthM).
- Mere sobe su unutrašnje (od zida do zida). Debljina zida se ne uračunava u dužinu i širinu.
- xM i yM jednaki null znače da soba još nema položaj na planu. To nije isto što i (0, 0).
- Položaj sobe na planu je raspored koji bira korisnik. Nije izmerena vrednost i ne pokazuje stvarne
  razmake između soba dok to korisnik ne potvrdi.
- Za prikaz se metri pretvaraju u centimetre ili milimetre, ali se u modelu uvek čuvaju u metrima.

## Zidovi i otvori (vrata i prozori)

Svaka soba ima četiri zida, a svaki zid se zove po strani sveta:

| Zid | Gde je | Dužina zida | Početak zida |
|---|---|---|---|
| NORTH (severni) | gornja ivica sobe | dužina sobe | zapadni (levi) kraj |
| SOUTH (južni) | donja ivica sobe | dužina sobe | zapadni (levi) kraj |
| WEST (zapadni) | leva ivica sobe | širina sobe | severni (gornji) kraj |
| EAST (istočni) | desna ivica sobe | širina sobe | severni (gornji) kraj |

- Otvor (vrata ili prozor) pripada tačno jednoj sobi i jednom njenom zidu.
- offsetM je rastojanje od početka zida do bližeg kraja otvora, a widthM je širina otvora duž zida.
  Otvor zauzima deo zida od offsetM do offsetM + widthM.
- sillHeightM je visina donje ivice otvora od poda. Za vrata je uvek 0.
- Vrata imaju još i swing (otvaraju se u sobu ili iz sobe) i hinge (šarke su na početku ili na kraju otvora,
  gledano duž zida od njegovog početka).
- Otvor mora da staje u zid (offsetM + widthM ne sme preći dužinu zida), da staje u visinu sobe
  (sillHeightM + heightM ne sme preći visinu zida) i ne sme da se preklapa sa drugim otvorom na istom zidu.
  Otvori koji se samo dodiruju krajevima nisu preklopljeni.
- Otvor koji spaja dve sobe (prolaz kroz zajednički zid) još nije u modelu. Za sada svaki otvor pripada jednoj sobi.
