# Max-Profit-Data-Preservation-Simulation

---
A simulation for testing data preservation of base station-less networks (BSNs) while trying to maximize profit.

## Table of Contents

---
- [About](#about)
- [Setup](#setup)
- [Example](#example)
  - [Terminal Output](#terminal-output)
- [Authors](#authors)

## About

---
This data preservation simulation uses the project [grivera64/Sensor-Network-with-Max-Profit](https://github.com/grivera64/Sensor-Generator-with-Max-Profit)
to generate a suite of base station-less networks (BSNs) and applies several
multi-agent policies, such as Greedy and Multi-agent Nash Q-Learning, in comparison to the optimal solution
using Goldberg's CS2 program.

This simulation outputs the energy consumption of using the above algorithms, denoted by E_strategy.

This simulation was used jointly in our paper "Nash Equilibria of Data Preservation in Base Station-less Sensor Networks" by Giovanni Rivera,
Yutian Chen, and Bin Tang, pp. 6-7. for analyzing the Rate of Efficiency Loss (REL) for each of these strategies in compared
to the optimal energy consumption from the Minimum Cost Flow algorithm (More information can be found in [grivera64/Sensor-Network-with-MCF](https://github.com/grivera64/Sensor-Generator-with-MCF)).

## Setup

---

### Dependencies

- JDK 17 or newer ([Latest JDK from Oracle](https://www.oracle.com/java/technologies/downloads/))
- CS2 executable ([Installation and Setup Instructions](./CS2_SETUP.md))
  - Use `make` to build the binaries from the repository above and save the executable somewhere and remeber its path.
  > Note: We will refer to the path to the CS2 executable as `%PATH_TO_CS2%`.
- Google Guava Library ([GitHub Release Download Links here](https://github.com/google/guava/releases/))

### 1. Clone the Repository

Open a command line or terminal instance and enter the following command:
```sh
git clone https://github.com/grivera64/Max-Profit-Data-Preservation-Simulation.git
```

You can also download the repository as a zip file directly
from GitHub [here](https://github.com/grivera64/Data-Preservation-Simulation/archive/refs/heads/main.zip) and unzip it.

### 2. Change directories into the source folder.

```sh
cd Max-Profit-Data-Preservation-Simulation
cd src
```

### 3. Compile using javac

Windows:
```batch
javac -cp ".;%PATH_TO_GUAVA%/*" -p . *.java -d ../bin
```

Mac/Linux:
```sh
javac -cp ".:${PATH_TO_GUAVA}/*" -p . *.java -d ../bin
```

### 4. Change directories into the binaries folder

```sh
cd ..
cd bin
```

### 5. Run the program

Windows:
```batch
java -cp ".;%PATH_TO_GUAVA%/*" -p . RunModelTests
```

Mac/Linux:
```sh
java -cp ".:${PATH_TO_GUAVA}/*" -p . RunModelTests
```

## Example

---
### Terminal output

```txt
Welcome to the Max Profit Data Preservation Simulator!
===========================================

Please enter an option: (G)enerate/(F)ile/(Q)uit:
> F
Please enter the file name:
F > figure_3_sensor_network.sn

Where is your installation of cs2.exe located?
(".") > $PATH_TO_CS2

How many episodes should MARL run?
(100) > 

Running models...
=================
Greedy:
Cost: 6412 µJ
Profit: 9452 µJ

DN01 -> SN03 (flow = 2)
	[DN01 -> SN03]
DN02 -> SN04 (flow = 2)
	[DN02 -> SN04]
DN03 -> SN02 (flow = 2)
	[DN03 -> SN02]
DN04 -> SN01 (flow = 2)
	[DN04 -> SN01]
DN05 -> SN05 (flow = 2)
	[DN05 -> SN05]

CS2 (Optimal):
Saved flow network in file "cs2_tmp_20230520150722.inp"!
Cost: 6406 µJ
Profit: 9458 µJ

DN01 -> SN03 (flow = 2)
	[DN01 -> SN03]
DN02 -> SN04 (flow = 2)
	[DN02 -> SN04]
DN03 -> SN05 (flow = 2)
	[DN03 -> SN05]
DN04 -> SN01 (flow = 2)
	[DN04 -> SN01]
DN05 -> SN02 (flow = 2)
	[DN05 -> SN02]

MARL (100 episodes):
Cost: 6406 µJ
Profit: 9458 µJ

DN01 -> SN03
	[DN01 -> SN03]
DN01 -> SN03
	[DN01 -> SN03]
DN02 -> SN04
	[DN02 -> SN04]
DN02 -> SN04
	[DN02 -> SN04]
DN03 -> SN05
	[DN03 -> SN05]
DN03 -> SN05
	[DN03 -> SN05]
DN04 -> SN01
	[DN04 -> SN01]
DN04 -> SN01
	[DN04 -> SN01]
DN05 -> SN02
	[DN05 -> SN02]
DN05 -> SN02
	[DN05 -> SN02]

```

## Related Projects

---
- Sensor Generator with Max Profit ([grivera64/Sensor-Generator-with-Max-Profit](https://github.com/grivera64/Sensor-Generator-with-Max-Profit))
  - Sensor Network generator visualizer for CS2 Min-Cost Flow input for maximizing profit in data preservation.
  - By Giovanni Rivera ([@grivera64](https://github.com/grivera64))

- Sensor Generator with MCF ([grivera64/Sensor-Generator-with-MCF](https://github.com/grivera64/Sensor-Generator-with-MCF))
  - Original Sensor Network generator and visualizer for CS2 Min-Cost Flow input that we forked for [grivera64/Sensor-Generator-with-Max-Profit](https://github.com/grivera64/Sensor-Generator-with-Max-Profit).
  - By Giovanni Rivera ([@grivera64](https://github.com/grivera64))

- Data Preservation Simulation ([grivera64/Data-Preservation-Simulation](https://github.com/grivera64/Data-Preservation-Simulation))
  - A simulation for testing data preservation of base station-less networks (BSNs).
  - By Giovanni Rivera ([@grivera64](https://github.com/grivera64))

## Authors

---
- Giovanni Rivera ([@grivera64](https://github.com/grivera64))
- Christopher Gonzalez ([@chrisagonza97](https://github.com/chrisagonza97))
