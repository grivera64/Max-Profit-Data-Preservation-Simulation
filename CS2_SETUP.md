# CS2-Setup

## Pre-requisites
- gcc
- make (or mingw32-make for Windows)

## Setup
1. Clone the CS2 repository from GitHub.

```sh
git clone https://www.github.com/iveney/cs2
```

2. Change directories into the CS2 folder where all of the source code is.

```sh
cd cs2
```

3. In the cs2 folder we cloned, there will be a `makefile` located inside of it. Open
   it in the text editor of your choice and copy and paste the following code:

```makefile
# makefile for cs2
# compileler definitions
# COMP_DUALS to compute prices
# PRINT_ANS to print flow (and prices if COMP_DUAL defined)
# COST_RESTART to be able to restart after a cost function change
# NO_ZERO_CYCLES finds an opeimal flow with no zero-cost cycles
# CHECK_SOLUTION check feasibility/optimality. HIGH OVERHEAD!

# change these to suit your system
CCOMP = gcc
CFLAGS = -g -Wall
BIN=cs2.exe

OS = $(shell uname)
ifeq ($(OS), Windows_NT)
    CDEFINES = -DWINDOWS_TIMER -DPRINT_ANS -DCOMP_DUALS -DCOST_RESTART
else
    CDEFINES = -DPRINT_ANS -DCOMP_DUALS -DCOST_RESTART
endif

cs2.exe: cs2.c parser_cs2.c types_cs2.h timer.c
    $(CCOMP) $(CFLAGS) $(CDEFINES) -o $(BIN) cs2.c -lm

```

4. Build the source files into your output executable using `make`.

Windows:
```bat
mingw32-make
```

Mac/Linux:
```sh
make
```

5. You can now either
- Copy the absolute path of the enclosing folder of the executable. You will need this when running `RunModelTests`. 

    > **TIP**: You can use the `pwd` command on Mac/Linux to see the current path.

- Copy the executable and paste the executable to the folder you need it in.
