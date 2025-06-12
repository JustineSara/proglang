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
clj -X main/run
```
run the `run` function of `main` namespace. It currenlty takes no input. input should be a list of keys and values that are passed as a map to the clojure function (I think).

## Project organisation

Folders:
- `bin`: storing useful piece of bech code
- `deps.edn`: where we list clojure dependencies, paths and aliases
- `nix`: nix stuff
- `shell.nix`: the file where you can add dependencies like `Clojure`
- `src`: where the clojure files are
    - `hello_instaparse.clj`: testing `instaparse` is usable
    - `main.clj`: where I code the project

## Steps

### Step 1

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

### Step 2

**Goal**: add the maths-parenthesis (priority of operations) to the grammar.

**Solution**: I define the piece of grammar `Ap` (resp. `Mp`) for addition with parenthesis (resp. multiplication), which are easily defined from `A` (resp. `M`) then we use the angle brackets `<Ap>` so the tag does not appear in the results.

### Step 3 : eval

**Goal**: turn the tree into a value.

**Solution**: I built `opeeval` which has a `string` input and outputs an `int` that is the result of the operation. It is built using the previous steps. We use `edn/read-string` rather than `parse-double` to stay within `int` format.

### Step 4 : shell

**Goal**: create a shell so a user can write operations and have the result.

**Solution - part 1**: in the main function called `run` we create a loop that takes the user input, run it through our eval and then print the result. To start the program, use the command line `clj -X main/run`.

**But**: for the moment, we can quit the program by `ctr+c` which actually stop the whole clojure. Let's add an offical command to stop it so our loop is not quite infinite.

**Solution - part 2**: we add to the grammar so the commands `q!` and `quit!` are recognised, then the tree is also adapted and we have a branching in the main program so that it knows what to do with this new possible output (not printing it).
