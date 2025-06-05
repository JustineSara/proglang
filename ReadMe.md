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


## Project organisation

Folders:
- `bin`: storing useful piece of bech code
- `deps.edn`: where we list clojure dependencies, paths and aliases
- `nix`: nix stuff
- `shell.nix`: the file where you can add dependencies like `Clojure`
- `src`: where the clojure files are
    - `hello_instaparse.clj`: testing `instaparse` is usable
