# Upravnik Predstavnik README

- [ ] TODO Replace or update this README with instructions relevant to your application

To start the application in development mode, import it into your IDE and run the `Application` class. 
You can also start the application from the command line by running: 

```bash
./mvnw
```

To build the application in production mode, run:

```bash
./mvnw -Pproduction package
```

## Getting Started

The [Getting Started](https://vaadin.com/docs/latest/getting-started) guide will quickly familiarize you with your new
Upravnik Predstavnik implementation. You'll learn how to set up your development environment, understand the project 
structure, and find resources to help you add muscles to your skeleton — transforming it into a fully-featured 
application.



# NGROK


ngrok http 8080











ngrok config add-authtoken 2yMrc9GeldRI1W1dvHNIbdz3GCD_5byvFz95WEcdaDK8pAVV

https://18e2-89-212-58-150.ngrok-free.app





Prav imate, naleteli ste na posodobljene varnostne omejitve Googla. Oprostite za mojo prejšnjo informacijo, ki ni upoštevala teh strožjih pravil.

Google je pred časom poostril varnostne zahteve in ne dovoli več uporabe surovih lokalnih ali javnih IP naslovov v "Authorised redirect URIs". Izjema je localhost, ki je posebej obravnavan za namene lokalnega razvoja.

Sporočili, ki ste ju prejeli, pomenita točno to:

Must end with a public top-level domain: URI se mora končati z javno domeno najvišje ravni (npr. .com, .org, .si). 192.168.1.5 ni domena.
must use a domain that is a valid Top private domain: Uporabiti je treba veljavno domeno.
Rešitev: Uporaba tunelske storitve (npr. ngrok) tunneling
Najbolj enostavna in pogosta rešitev za testiranje na mobilni napravi je uporaba storitve, ki ustvari varen javni URL in ga preusmeri na vaš lokalni strežnik (localhost). Najbolj priljubljena za to je ngrok.

To vam omogoča, da vašo lokalno aplikacijo začasno izpostavite na spletu preko veljavne in varne (https) domene, ki jo bo Google sprejel.

Koraki so naslednji:

Prenesite in namestite ngrok: Pojdite na uradno stran ngrok in sledite navodilom za namestitev.

Zaženite lokalni strežnik: Prepričajte se, da vaša aplikacija teče na localhost:8080.

Zaženite ngrok: V terminalu (ukazni vrstici) poženite naslednji ukaz, da ustvarite tunel do vašega lokalnega porta 8080:

Bash

ngrok http 8080
Kopirajte ngrok URL: Ko se ngrok zažene, vam bo v terminalu izpisal nekaj informacij, med drugim tudi "Forwarding" URL, ki bo izgledal nekako takole:
https://random-string-1234.ngrok-free.app


Dodajte nov URI v Google Cloud Console:

Pojdite nazaj v nastavitve vašega OAuth 2.0 Client ID.
V "Authorised redirect URIs" dodajte nov URI, sestavljen iz ngrok naslova in vaše poti za preusmeritev: https://random-string-1234.ngrok-free.app/login/oauth2/code/google
POMEMBNO: Uporabite https naslov, ki vam ga da ngrok. Zamenjajte random-string-1234.ngrok-free.app z vašim dejanskim ngrok podnaslovom.
Testirajte na telefonu:

Shrani spremembe v Google Cloud Console.
Na svojem telefonu sedaj odprite brskalnik in vnesite ngrok naslov (https://random-string-1234.ngrok-free.app).
S tem boste dostopali do vaše aplikacije, ki teče lokalno na računalniku, in Google prijava bo delovala brezhibno, saj bo preusmeritev potekala na veljavno domeno.
To je standardna praksa za razvoj in testiranje spletnih aplikacij, ki zahtevajo OAuth avtentikacijo na različnih napravah.