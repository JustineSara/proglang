# ProgLang

A small project to explore creating a programming language

## Project Set Up

- This is coded in `Clojure`.
- It is intalled using `Nix` and `direnv` with the `nix.shell` file
- Dependencies are handled using `deps.edn`
- the core of the code is stored in `src`

### Running stuff

```
$ clj -M:cider-clj
```
to start the repl used by vim

```
$ clj
```
to start a repl where you can require the librairies or namespaces defined in `src`

```
clj -X:test
```
to run the tests

```
clj -M -m main [file]
```
run the `-main` function of `main` namespace. with no argument it starts a shell. With one argument, it reads a file and runs the program in the file.

```
clj -T:build uberjar
```
creates the file `target/proglang.jar` which can be distributed.

```
java -jar target/proglang.jar
```
to run the proglang program / uberjar.

```
clj -T:build clean
```
clean up the build.


## Project organisation

Folders:
- `bin`: storing useful piece of bech code
- `build.clj`: code to build the `.jar` for our project
- `deps.edn`: where we list clojure dependencies, paths and aliases
- `nix`: nix stuff
- `shell.nix`: the file where you can add dependencies like `Clojure`
- `src`: where the clojure files are
    - `hello_instaparse.clj`: testing `instaparse` is usable
    - `main.clj`: where I code the project

## Steps

### Step 1: do basic maths

**Goal**: code the grammar part. The input is an instruction string, the output is a tree representing the instruction. We are limiting this to additions and multiplications (which should be computed in the order expected form conventional maths, no parenthesis) and to integers.

**Results**: (see `step1` in `test`) a string with "+" and "\*" are now turned into a tree that is nested vectors where `:S` is the whole thing `:A` is for additions between all members of the current vector and `:M` is the same but with multiplication. The :D tag means it is an integer.

For example:
```
"2*1 +1"
```
becomes
```
[:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]
```

### Step 2: more maths

**Goal**: add the maths-parenthesis (priority of operations) to the grammar.

**Solution**: I define the piece of grammar `Ap` (resp. `Mp`) for addition with parenthesis (resp. multiplication), which are easily defined from `A` (resp. `M`) then we use the angle brackets `<Ap>` so the tag does not appear in the results.

### Step 3: eval

**Goal**: turn the tree into a value.

**Solution**: I built `opeeval` which has a `string` input and outputs an `int` that is the result of the operation. It is built using the previous steps. We use `edn/read-string` rather than `parse-double` to stay within `int` format.

### Step 4: shell

**Goal**: create a shell so a user can write operations and have the result.

**Solution - part 1**: in the main function called `run` we create a loop that takes the user input, run it through our eval and then print the result. To start the program, use the command line `clj -X main/run`.

**But**: for the moment, we can quit the program by `ctr+c` which actually stop the whole clojure. Let's add an offical command to stop it so our loop is not quite infinite.

**Solution - part 2**: we add to the grammar so the commands `q!` and `quit!` are recognised, then the tree is also adapted and we have a branching in the main program so that it knows what to do with this new possible output (not printing it).

### Step 5: read code from file

**Goal**: we want to be able to run programs from a file.

**Part 1**: we set up a way to build the program with `build.clj` and the command line `clj -T:build uberjar`

**Part 2**: reworked the main so it handles the number of arguments and call the `run` or `run-file` functions. `run` is now not callable through `clj -X main/run` as it takes no input arguments now.

**Part 3**: completed the `run-file` function, checkink the file exists and then reading one line at a time and evaluating it. By default it only prints the last result. The options `-sao` and `-sai` shows all outputs and all inputs (and outputs).

### Step 6: handle multiline program

**Goal**: handle multiline programs through the grammar.

**Part 1**: modify the grammar and tree-making to add the newline and distinction between expression and statement (currently not there apart from the `quit!` command).

**Part 2**: make it work for running a file: need to account for the last newline at the end of the file. Note that currently empty lines make everything fail. Also removed the options as they are not relevant anymore.

### Step 7: assignement

**Goal**: be able to do `a=1` and then `2*a` and get `2`.

**Part 1**: add to the grammar so we accept `a=1`. (not doing anything with it for now).

**Part 1 also**: have the grammar distinguish between the name of a value during attribution `Aname` and during use `Rname` (resolve).

**Part 2**: eval the new definition. Need to add a memory, I use an `atom` `a`. In the shell we want to keep the saved values in between lines, which we evaluate one by one, so we have a memory map `m` that is used to initialise the `atom a`.

### Step 8: function definition

**Goal**: create `def <fname> (<args>):` so a function can be defined and also be called.

**Part 1 - grammar 1**: add the function definition to the grammar

**Part 2 - grammar 2**: add calling the function to the grammar

**Challenge**: the current evaluation starts from the most-inside node, which does not work
with function-definition. So I rewrite the `opeeval` function into `node-eval` that
is called recursively.

**Note**: if the function use an outside variable, the value used is the one
defined when the function is called rather than when it is defined:
```
b = 0
def addb(a):
  return a+b

addb(2)
>    2

b =2

addb(2)
>    4
```

**Part 3 - eval**: in the new `node-eval` I code the resolution of the function. There in an "in-function memory" separated form the global `m` so they don't contaminate each other. We first solve the args, add them to that in-function memory and resolve the lines of the functions one by one until we reach `return` (or the end, upon which we have an error)
