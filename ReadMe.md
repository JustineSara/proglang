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
