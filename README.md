# Tres en Ratlla (Tic-Tac-Toe) amb MVVM



**Autor:** Alan Aparicio Carretero

**Curs:** DAM2 - Institut Provençana

**Repositori:** `Android-MVVM-Tic-Tac-Toe`

## 1. Anàlisi i documentació de l'arquitectura MVVM



### 1.1. Documentació dels components (basant-nos en MVVMSimple)



* **Model:** Es l'estructura de dades que guarda l'estat de la pantalla (`SimpleState`, amb el comptador `counter` i el text `message`). La seva funció principal es emmagatzemar les dades de forma immutable sense barrejar res de la interficie grafica.


* **ViewModel:** S'encarrega de gestionar l'estat de la pantalla i processar les accions de l'usuari (`incrementCounter()` i `reset()`). Es comunica amb el Model creant copies noves amb `.copy()` cada vegada que canvia una dada, i exposa l'estat a la vista amb una variable publica de només lectura (`State<SimpleState>`) per evitar que la UI el pugui modificar directament.


* **View (UI/Compose):** Es la part visual que veu l'usuari (`MainActivity` i la funció `@Composable SimpleScreen`). Rep els canvis d'estat llegint `viewModel.state.value` (quan aquest valor canvia, Compose repinta la pantalla sol) i avisa al ViewModel executant els seus metodes dins del `onClick` dels botons.



### 1.2. Anàlisi comparatiu (MVVMSimple vs MVVM-separatModel)



1. **Estructura i Separació de Responsabilitats:**


* A `MVVMSimple`, el Model es nomes una `data class` bàsica per guardar variables. En canvi, a `MVVM-separatModel`, el Model es divideix en classes amb responsabilitat pròpia (`CardModel` i `BoardModel`) i incorpora un metode de fàbrica (`createInitialBoard()`) per inicialitzar la partida.


* A `MVVMSimple`, la lògica de negoci s'executa directament dins del ViewModel. A `MVVM-separatModel`, tota la lògica del joc (girar cartes amb `onCardClick` o comprovar parelles amb `checkMatches`) s'executa dins del propi `BoardModel`, deixant el ViewModel només per gestionar l'estat reactiu i els `delay` amb corrutines.




2. **Immutabilitat i Gestió de l'Estat:**


* A `MVVM-separatModel` no es modifiquen mai les llistes ni les propietats directament. Es fa servir el metode `.copy()` per retornar un objecte `BoardModel` o `CardModel` nou amb els valors actualitzats. Això es fonamental en una arquitectura reactiva perquè Compose detecta que la referència de l'objecte ha canviat i dispara la recomposició de la pantalla sense errors ni efectes secundaris.




3. **Avantatges de la separació:**


* Separar la lògica de negoci (`BoardModel`) del ViewModel evita que el ViewModel es converteixi en un fitxer gegant impossible de mantenir quan el joc creix. A més, permet provar i reutilitzar les regles del joc en Kotlin pur sense dependre de les llibreries d'Android.





### 1.3. Anàlisi comparatiu (StateFlow vs mutableStateOf)



* **Per a què es fan servir:**


* `StateFlow / MutableStateFlow`: Es un flux de dades de Kotlin Coroutines que guarda l'estat actual i emet els canvis als observadors de forma reactiva.


* `mutableStateOf / State<T>`: Es el sistema d'estat natiu de Jetpack Compose pensat per guardar valors observables directament per la UI.




* **Diferències entre ells:** `mutableStateOf` depèn directament de les llibreries de Compose i es llegeix amb `.value`, mentre que `StateFlow` es part de Kotlin Coroutines (independent de la UI), permet treballar millor amb tasques asíncrones (`viewModelScope`) i a la vista es llegeix amb `collectAsState()`.


* **Quin he escollit i per què:** He escollit **`StateFlow / MutableStateFlow`** perquè és el que demana l'arquitectura del projecte, desacobla totalment el `GameViewModel` de Compose i és la forma més neta d'exposar l'estat en aplicacions Android modernes.



## 2. Arquitectura del meu Tres en Ratlla i Funcionament



He desenvolupat el joc aplicant el patró de **Model separat amb lògica pròpia** dins del paquet `org.proven.tresenratlla`:

* **Model (`CellModel` i `BoardModel`):**


* `CellModel`: Guarda l'`id`, la `position` (0 a 8) i el `symbol` (`"X"`, `"O"` o buit `""`), amb el mètode `placeSymbol()`.


* `BoardModel`: Conté la llista de les 9 caselles (`cells`), el torn actual (`currentTurn`), el guanyador (`winner`), si hi ha empat (`isDraw`) i si la partida ha acabat (`isGameOver`). Inclou el mètode de fàbrica `createInitialBoard()` i tota la lògica pura del joc a `makeMove(position: Int)` i `checkWinner()`, retornant sempre noves instàncies amb `.copy()`.




* **ViewModel (`GameViewModel`):**


* Manté l'estat intern del tauler en un `MutableStateFlow` privat (`_board`) i l'exposa a la vista com a només lectura amb `asStateFlow()` (`board`).


* Connecta les accions de l'usuari amb el model mitjançant `onCellClick(position: Int)` i `restartGame()`.




* **View (Jetpack Compose):**


* `GameScreen`: Llegeix l'estat del tauler en temps real amb `collectAsState()`, mostra de qui és el torn o qui ha guanyat, i inclou el botó de reinici.


* `BoardGrid` i `CellItemView`: Dibuixen la graella de 3x3 utilitzant `LazyVerticalGrid` amb `GridCells.Fixed(3)` i targetes clicables per a cada casella.





<img width="370" height="823" alt="{C4FB7E44-2683-4161-A2F3-40844F351868}" src="https://github.com/user-attachments/assets/9020ce77-d2b4-4278-8873-50059e06ac7c" />




## 3. Conceptes i Mètodes Apresos



* **`collectAsState()`:** Permet escoltar un `StateFlow` del ViewModel dins d'una funció `@Composable` i convertir-lo en un estat que fa que la pantalla s'actualitzi sola cada cop que fem un moviment.


* **`LazyVerticalGrid` i `GridCells.Fixed(3)`:** Component de Compose per muntar quadrícules de forma ràpida; fixant 3 columnes es crea directament l'estructura 3x3 del tauler sense haver d'anidar múltiples `Row` i `Column`.


* **`MutableStateFlow` i `asStateFlow()`:** Serveixen per protegir l'estat dins del ViewModel: el `MutableStateFlow` és privat per poder canviar el valor internament, i amb `asStateFlow()` el fem públic però bloquejant que la vista el pugui sobreescriure.


* **`.copy()`:** Funció pròpia de les `data class` de Kotlin que ens permet clonar el tauler o una casella canviant només les dades que ens interessen (com el símbol o el torn) mantenint la immutabilitat que necessita Compose per detectar els canvis.


* **`by viewModels()`:** Delegat d'Android que instancia el `GameViewModel` associat a la `MainActivity` i conserva l'estat de la partida encara que girem la pantalla del mòbil.
