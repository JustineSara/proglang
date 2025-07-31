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

**Challenge**: Nick's favourite test where we define a function within a function.
- I first reworked the memory so that I do not need to know if the element is a value or a function to retrieve it.
- I identified the issue as such (11th July): I need to replace named-elements by their value when those are defined inside the function, but not if they are defined outside of the function, but I do not know how to distinguish between the two. It goes further than that with Nick's test.
- Discussing with Gary (who worked this out weeks earlier), I need to replace nothing but save in the result the sub-memory defined within the function. Although my brain is certain there is a "simpler" way I have not been able to articulate it yet, so I am going to implement this solution instead.

**Part 4 - memories (plural)**: Implementing "namespace" or different "level of memory": everything will be passed from line to line, a function will also now be associated to a memory within which some value might be defined. (pretty sure it's not clear but well ...) Each memory excetp the `global` one refers to another `higher level` memory, so a function has its memory refering to `global`. A function within a function has a memory refing to the first function's memory space which then refer to the `global` one. It is then possible to explore memories in order to find values when we run the function.

The `eval-node` function now consistently returns `[m m-lvl res]`with:
- `m` : all the memories
- `m-lvl` : the memory within which we are currenlty working
- `res` : the result in the form of a map with the `:type` (and the rest based on the type)

**Clean up and Test**: done :) only the `run` function is not active anymore. I need to decide how to handle multi-lines (or not allow it) when in the shell.


### Step 9: IF and then

**Goal**: implement `if` and `then` (`then` being facultative).

**Remark**: at the moment we have no booleans defined (we most likely will do so in the future), so we use the value `0` as `false` and everything else as `true`

**step 1: grammar and trouble** Adding `if` and `else` in the grammar at first is simple. But, we want things to work when `else` is not defined, **and** we want things to work when there are `if`-s within `if`-s.

But my current grammar does not count the size of the indent before any lines of code, it works for functions, but here is the glictch:
```python
if 0:
  if 0:
    a=1
  else:
    a=2
```
is interpreted by the grammar as if the `else` belonged to the first `if` rather than the first.

To solve this I try using the _look-ahead_ capacity of [`instaparse`][insta]:
I want my `else` to be precedeed by `'  '{m} if <other stuff for if> '\n' '  '{m}` with the two `m`-s being the same.
I can define that with a form of recursion with `if0 = if <blabla>: <blibli> \n` then `if1 = '  ' if0 '  '`, `if2 = '  ' if1 '  '`, and so on. With `else` being precedeed by any `ifx`, except ... I do not know how to express this so it covers _all_ `ifx`.


The other option is to do as Gary does: count the space and re-organise (and check) the grammar result based on those blocks of same-indentation. (aka: when being lazy earlier caught up with me and I reach the limit of the grammar approach)


We decided to checkout the [python grammar][pyGRAMM] but it did not help me that much. It confirmed that they detect whole block at a time as I am trying to do. They also make use of look-ahead, negative look-ahead and order (like I am trying to do). Although it did not directly helped, it led us to the idea of handling level of indentation in pre-grammar rather than post-grammar like Gary is doing.

Basically:
* _my current way but it has a problem_: do everything with grammar and then compute the results.
* _Gary's way of doing thing_: first apply the grammar which also detects the indent information; then post-process the tree to create the indented-blocks; and last compute results (Idea: Gary use the grammar and counts the ident, we could use the meta-data give by instaparse to do the same whitout re-working the grammar too much)
* _new idea_: read line by line: for each line, check the indent (either match the expected indent level or go back one level closing the previous block); then feed the line (without the indent) to the grammar; do that until we have full blocks (for shell) or run the whole text (for files) and then compute

Another idea is to use `insta/parses` that shows all possible results of one grammar when it is ambiguous. Then we could use the meta-data of both to identify which is correct = to which `if` the lonely `else` is assigned. This is closer to what Gary does with post-processing and might be also heavier in computation as we asked [`instaparse`][insta] to look for all possibilities (which might become a lot for bigger codes). So, let's not do this.
```Clojure
user=> (require '[instaparse.core :as insta])
nil
user=> (require '[main :reload true])
nil
user=> (in-ns 'main)
#object[clojure.lang.Namespace 0x13868c41 "main"]
main=> (insta/parses ope "if 0:\n  if 0:\n    a=1\n  else:\n    a=2\n")
([:S [:if [:D "0"] [:flines [:if [:D "0"] [:flines [:assign [:Aname "a"] [:D "1"]]]]] [:else [:flines [:assign [:Aname "a"] [:D "2"]]]]]] [:S [:if [:D "0"] [:flines [:if [:D "0"] [:flines [:assign [:Aname "a"] [:D "1"]]] [:else [:flines [:assign [:Aname "a"] [:D "2"]]]]]]]])
```

Another option is to decide that every `if` needs its `else` and thus the earlier issue should not arise. It would be easy to make sure an `if` is always matched with an `else` in the grammar and move on. However, that means that the indent itself is not meaningful (it's not much meaningful at the moment and is thus the problem I am facing now).
Even with this constraint, the indents need to be taken into account to differenciate between thos two situations:
```Python "Situation 1"
if A:
  if B:
    a = 1
  else:
    a = 2
  b = 10
else:
  b = 20
```
and
```Python "Situation 2"
if A:
  if B:
    a = 1
  else:
    a = 2
    b = 10
else:
  b = 20
```

In conclusion, I need to explicitely deal with indents and count them (the horror!).

I will try to implement the new idea we had, doing it with a pre-grammar process.
(Note: at this juncture and for this commit, test has two failures due to the issue described above)

**step 2**: new grammar aand block management. I have:
- a new grammar that manages only one line at a time. It always returns [:S [:INDENT ...] [...]]
- a block-creating function that uses check the indents and arrange the tree based on those
- a `from-text-to-gram` function that takes the text, splits it by lines and calls the block-creating function and return only the tree.

At this stage I am not handlling blank lines, which means the last uncommented test does fail.


[insta][https://github.com/engelberg/instaparse]
[pyGRAMM][https://docs.python.org/3/reference/grammar.html]
