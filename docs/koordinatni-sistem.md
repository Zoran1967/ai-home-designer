# Koordinatni sistem projekta

- Jedinica: metar. Sve arhitektonske mere i koordinate u modelu su u metrima.
- Početak (0, 0): proizvoljna tačka plana. Nije vezana za zgradu.
- Osa x ide udesno, osa y ide nadole (kao na ekranu telefona). Pozitivni uglovi se računaju od x ose ka y osi.
- Soba je pravougaonik čiji je položaj gornji levi ugao unutrašnjosti sobe (xM, yM).
  Dužina (lengthM) ide duž ose x, a širina (widthM) duž ose y.
  Soba zauzima područje od (xM, yM) do (xM + lengthM, yM + widthM).
- Mere sobe su unutrašnje (od zida do zida). Debljina zida se ne uračunava u dužinu i širinu.
- xM i yM jednaki null znače da soba još nema položaj na planu. To nije isto što i (0, 0).
- Položaj sobe na planu je raspored koji bira korisnik. Nije izmerena vrednost i ne pokazuje stvarne
  razmake između soba dok to korisnik ne potvrdi.
- Za prikaz se metri pretvaraju u centimetre ili milimetre, ali se u modelu uvek čuvaju u metrima.
