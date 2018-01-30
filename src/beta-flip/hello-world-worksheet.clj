;; gorilla-repl.fileformat = 1

;; **
;;; # An Anglican Probabilistic Hello World
;;; 
;;; This file is a [Gorilla Repl](http://gorilla-repl.org/index.html) worksheet. This is a notebook format which allows writing Clojure (and Anglican) code in cells within a document. Conceptually this is quite similar to (e.g.) iPython notebooks.
;;; 
;;; Shift + enter evaluates a code block. Hit ctrl+g twice in quick succession or click the menu icon (upper-right corner) for more commands.
;; **

;; **
;;; This worksheet, written by
;;; 
;;; - Frank Wood : [fwood@robots.ox.ac.uk](mailto:fwood@robots.ox.ac.uk)
;;; - Brooks Paige : [brooks@robots.ox.ac.uk](mailto:brooks@robots.ox.ac.uk)
;;; 
;;; is designed to introduce you to the basics of Clojure and Anglican.
;;; It introduces probabilistic programming inference in a "Hello World!" Beta-Bernoulli model, illustrates different ways of writing queries, and shows different ways of consuming their output.
;;; 
;;; 
;;; The following cell defines a _namespace_, and imports some functions we will need. This is a Clojure concept somewhat analogous to a class in Java, or a module in Python. For now, take this as given; we will supply necessary imports at the top of the document for all the examples.
;;; 
;;; Run it by clicking on it and hitting shift+enter. This block of code often takes 10 seconds or more to run while Clojure unpacks and initializes dependencies.
;;; 
;;; Output will appear just below the cell; in this case we expect `nil`.
;; **

;; @@
(ns hello-world
  (:require [anglican importance lmh]
            [gorilla-plot.core :as plot]
            [anglican.stat :as stat])
  (:use [anglican core runtime emit
           [inference :only [infer collect-by equalize]]
           [state :only [get-predicts get-log-weight set-log-weight]]]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ## A quick tour of Clojure
;;; 
;;; Syntactically, Clojure is a type of LISP. This means that parenthesis are used for function application: the first element in a parenthesized sequence is a function, and the following elements are its arguments.
;;; 
;;; It can take a few minutes to become accustomed to this sort of prefix notation. The following cell demonstrates a series of standard arithmetic and mathematical expressions. 
;;; 
;;; Run it by clicking on it and hitting shift+enter. Output will appear just below the cell. Comments in Clojure begin with semicolons, and describe the code segments below.
;;; 
;; **

;; @@
;; add two numbers
(+ 1 1)

;; subtract: "10 - 3"
(- 10 3)

;; multiply, divide
(* 2 5)
(/ 10.0 3.3)

;; Complex arithmetic expressions are built up like so:
;; (10 * (2.1 + 4.3) / 2)
(/ (* 10 (+ 2.1 4.3)) 2)

;; Anglican supplies functions for `log`, `exp`, and more:
(exp -2)
(log (+ 1 1))
(sqrt 5)
;; @@

;; **
;;; Clojure is _typed_ and performs type conversion behind the scenes, almost always nicely.  It has types for floating-point numbers, integers, fractions, and booleans.  Clojure also has matrix types, but we won't need them for these exercises, though Anglican supports them (e.g. [Kalman smoother](http://www.robots.ox.ac.uk/~fwood/anglican/examples/viewer/?worksheet=kalman).)
;;; 
;;; Comparison operators `<`, `>`, `=`, `<=`, `>=` behave as one would expect, and can be used within an `if` statement. The `if` statement takes the form
;;; 
;;; `(if bool? expr-if-true expr-if-false)`
;;; 
;;; That is, an `if` expression will itself be a list with four elements: the first is `if`, the second evaluates to a boolean, and the last two are any arbitrary expressions. Here are a few examples. Please ask if you have any questions about these!
;;; 
;; **

;; @@
;; should output true
(< 4 10)

;; should output 1
(if (> 3 2) 1 -1)

;; should output 20
(if (<= 3 3) (+ 10 10) 0)

;; should output 4
(+ (if (< 4 5) 1 2) 3)
;; @@

;; **
;;; A `let` block is a bit of Clojure which can be used to define variables within a local scope. A `let` block takes an initial argument which defines a sequence of _bindings_, followed by a sequence of statements.
;;; 
;;; Bindings are a list in square-brackets `[]` of name-value pairs. In `(let [x 1 y 2] expr)`, the `expr` is evaluated with `x` set equal to 1, and `y` equal to 2.
;;; 
;;; If a `let` block includes multiple expressions, the return value of the entire block is the last expression. For more info see http://clojuredocs.org/clojure.core/let.
;; **

;; @@
;; prints 12
(let [x 10
      y 2]
  (+ x y))

;; ALSO prints 12!
(let [x 10
      y 2]
  (* x 3)
  (+ x y))

;; ... but this prints 32
(let [x 10
      y 2]
  (+ (* x 3) y))

;; ... and so does this
(let [x 10
      y 2
      x (* x 3)]
  (+ x y))

;; This has a side-effect, printing to the console,
;; which is carried out within the let block.
(let [x 10
      y 2]
  (println "x times 3 =" (* x 3))
  (+ x y))

;; There is also the `do` block, which is like let, but has no bindings:
(do 
  (println "10 =" 10)
  (println "1 + 1 ="  (+ 1 1)))
;; @@

;; **
;;; To do anything particularly interesting in Clojure, we will need functions, lists, and vectors.
;;; 
;;; There are two ways to define a function. The basic way is to use `fn`, which takes a list of argument names (in square brackets) and then a sequence of expressions (note that `defn` and `#` also create functions). It actually looks a lot like a `let` block! However, values for the arguments are passed in when the function is called. Here's an example:
;; **

;; @@
;; Define a function which takes x, y as inputs, then returns 2x + y + 3.
;; Then call that function on values x=5 and y=10, and return the result:
(let [my-fn (fn [x y] (+ (* 2 x) y 3))]
  (my-fn 5 10))
;; @@

;; **
;;; Here is some example usage of lists in Clojure:
;; **

;; @@
;; Create a list, explicitly:
(list 1 2 3)

;; Get the first element of a list.
;; This returns `1`, a number:
(first (list 1 2 3))

;; Get a list containing the "rest" of the list (all but the first element).
;; This returns `(2 3)`, a list:
(rest (list 1 2 3))

;; Check the length of a list using `count`:
(count (list 1 2 3 4))

;; Add an element to the FRONT of list with `conj`.
;; This returns `(0 1 2 3)`:
(conj (list 1 2 3) 0)

;; Create a list of 5 elements, all of which are the output of "1 + 1":
(repeat 5 (+ 1 1))

;; Create a list of integers in a certain range:
(range 5)
(range 2 8)

;; Create a list by repeatedly calling a function:
(repeatedly 3 (fn [] (+ 10 20)))
;; @@

;; **
;;; Here is some example usage of vectors in Clojure:
;; **

;; @@
;; Create a vector by using square brackets:
[1 2 3]

;; When using `conj` on a vector, the element is appended to the END of the vector.
;; This creates [1 2 3 4]:
(conj [1 2 3] 4)
;; @@

;; **
;;; One thing which will be useful are the `map` and `reduce` functions for performing operations on lists.
;;; 
;;; `map` takes a function and a list (or lists), and applies the function to every element in the list.
;;; 
;;; `reduce` takes a function and a list, applies the function recursively to the pairs of elements in the list; see the examples below, or its documentation [here](http://clojuredocs.org/clojure.core/reduce).
;; **

;; @@
;; Apply the function f(x) = x*x to every element of the list `(1 2 3 4)`:
(map (fn [x] (* x x)) 
     (list 1 2 3 4))

;; Here's a different way of writing the above:
(map (fn [x] (pow x 2)) (range 1 5))

;; Apply the function f(x,y) = x + 2y to the x values `(1 2 3)` and the y values `(10 9 8)`:
(map (fn [x y] (+ x (* 2 y)))
     [1 2 3]   ; these are values x1, x2, x3
     [10 9 8]) ; these are values y1, y2, y3

;; @@

;; **
;;; The final essential Clojure construct we will want for the exercises is `loop ... recur`. This allows us to easily write looping code.
;;; 
;;; `loop` specifies initial values for a set of names (similar to a `let`-block) and then `recur` passes new values in when running the next loop iteration. This is best demonstrated by example. There are some examples http://clojuredocs.org/clojure.core/loop, and below:
;; **

;; @@
;; Loop from x=1 until x=10, printing each x:
(loop [x 1]
  (if (<= x 10)
    (let [next-x (+ x 1)]
      (println x)
      (recur next-x))))


;; This code loops from x=10 down to x=0, 
;; and builds up a vector y containing the values of 2x:
(loop [x 10
       y []]
  (if (= x 0)
    y
    (recur (- x 1)
           (conj y (* 2 x)))))
;; @@

;; **
;;; ## Anglican basics
;;; 
;;; Now we are ready to start using Anglican itself.
;;; 
;;; Anglican introduces a number of random primitives to the language, for example `normal`. Calling `(normal mu std)`, with arguments `mu` and `std`, creates a _distribution object_. This distribution object can then be sampled from (e.g. `(sample (normal 0 1))` draws a standard normal random variate).
;;; 
;;; It can also be used to compute log probabilities with `observe`, for example `(observe (normal 0 1) 3)` returns the log probability of the value `3` under the distribution `(normal 0 1)`.
;;; 
;;; Below are some example random primitives; these are sufficient to solve the exercises.  A full list of built-in primitives can be found [here](http://www.robots.ox.ac.uk/~fwood/anglican/language/index.html).
;; **

;; @@
;; Draw from a normal distribution with mean 1 and standard deviation 2:
(sample (normal 1 2))

;; Flip a coin, which comes up `true` with probability 0.7, and false with probabilty 0.3:
(sample (flip 0.7))

;; Sample from a uniform distribution on the open interval (3, 10):
(sample (uniform-continuous 3 10))

;; Sample from a beta distribution with parameters a=2, b=3:
(sample (beta 2 3))

;; Sample from a binomial distribution with n=10 and p=0.4:
(sample (binomial 10 0.4))

;; Sample from a discrete distribution with probabilities [0.3 0.2 0.5] on 0, 1, 2:
(sample (discrete [0.3 0.2 0.5]))

;; `repeatedly` can be pretty useful, here.
;; Suppose we want to draw 10 samples from the same normal distribution:
(let [normal-dist (normal 1 2.2)]
  (repeatedly 10 (fn [] (sample normal-dist))))

;; The # symbol can be used as a shorthand for function definition.
;; The same code as the previous line can also be written like so:
(let [normal-dist (normal 1 2.2)]
  (repeatedly 10 #(sample normal-dist)))

;; Using observe: log p(x=3), where x ~ Normal(0, 1):
(observe (normal 0 1) 3)
;; @@

;; **
;;; Let's use Anglican (and what we've learned) to pose a simple statistical query under the model
;;; 
;;; $$\begin{align}\theta &\sim \mathrm{Beta}(5,3) \\\\
;;; x &\sim \mathrm{Bernoulli}(\theta)\end{align}$$
;;; 
;;; and ask 
;;; 
;;; $$p(\theta>0.7 | x = true).$$
;;; 
;;; For this we can easily look up and/or compute the ground truth = 
;;; 
;;; $$p(\theta>0.7 | x = true) = .448 = 1-\mathrm{betacdf}(6,3,0.7)$$
;;; 
;;; Probabilistic models written in Anglican are called `queries`, and are defined using `defquery`.
;;; 
;;; The `predict` function is used within queries to set up structured return values (this will be made clear in a moment). The syntax is `(predict :label value)`, and the `:` is used in Clojure to define a keyword which can then be used as a key in an associative map.
;;; 
;;; ** Within a query, using `observe` doesn't just compute the log-probability, but actually changes the distribution over program executions to those that generate the observed data with higher probability. **
;;; 
;;; The following program defines the statistical model above:
;; **

;; @@
(defquery one-flip [outcome]
  (let [theta (sample (beta 5 3))]
    (observe (flip theta) outcome)
    (predict :theta-gt-pt07 (> theta 0.7))))
;; @@

;; **
;;; Take a moment to make sure that code block makes sense! `defquery` looks a lot like a function definition, except the contents of the `defquery` are actually Anglican code, which is then _compiled_ into a computable representation of the posterior (think sampler).
;;; 
;;; The query is named `one-flip`, and it takes a single argument `outcome`.
;;; 
;;; The `let` block defines `theta` as a random sample from the distribution `(beta 5 3)`.
;;; 
;;; The `observe` statement asserts that we see `outcome` as data generated from `(flip theta)`.
;;; 
;;; The `predict` statement says we would like to predict a value, named `:theta-gt-pt07`, which is equal to the `true/false` value of the expression `(> theta 0.7)`.
;;; 
;;; Together, these four lines define our first Anglican program/query/model.
;;; 
;;; 
;; **

;; **
;;; ### Posterior sampling from queries
;;; 
;;; 
;;; The `conditional` function takes a query and returns a distribution object constructor (think of the returned object as a factory for conditional/parameterized distributions). It takes various optional arguments which are used to specify the algorithm used for posterior sampling. Sensible values for these are provided in all exercises, and all different options are described in the [inference algorithms documentation](http://www.robots.ox.ac.uk/~fwood/anglican/language/index.html).
;;; 
;;; The following line defines `one-flip-posterior` as a distribution constructor which will draw posterior samples from the distribution defined by our query above, using the Lightweight Metropolis-Hastings (`:lmh`) algorithm.
;; **

;; @@
(def one-flip-posterior (conditional one-flip :lmh))
;; @@

;; **
;;; The object we just created plays the same role as `normal`, `flip`, or other built-in distribution constructors (except one can only `sample` but not `observe` from distributions created using `conditional`).
;;; 
;;; To actually create the posterior distribution itself, we create a distribution object which takes the query argument `outcome`. 
;;; 
;;; This is analogous to how when creating a normal distribution we must specify the mean and standard deviation, e.g. `(normal 0 1)`. Here, we specify whether our one outcome was true or false.
;; **

;; @@
(def true-flip-posterior (one-flip-posterior true))
;; @@

;; **
;;; Now, we can draw samples just as we would draw samples from a distribution created by calling `(normal 0 1)`. A sample from a conditional distribution defined in this way returns a key-value map, where the keys are the same as those specified in the `predict` statements.
;;; 
;;; To index into a hashmap in Clojure, just use the key as a function.
;; **

;; @@
;; Draw one sample (returns a key-value map):
(sample true-flip-posterior)

;; Draw one sample, and get the value associated with :theta-gt-pt07 for this sample:
;; (returns `true` or `false`)
(:theta-gt-pt07 (sample true-flip-posterior))
;; @@

;; **
;;; Sampling repeatedly from this distribution object characterizes the distribution.
;;; 
;;; Here, we're using the clojure builtin `frequencies`, and drawing 1000 samples.
;; **

;; @@
(frequencies (repeatedly 
               1000
               #(sample true-flip-posterior)))
;; @@

;; **
;;; A rudimentary plotting capability comes as part of [Gorilla REPL](http://gorilla-repl.org/).  Here we use a histogram plot to show the estimated distribution.
;; **

;; @@
(plot/histogram 
  (->> (repeatedly 10000 #(sample true-flip-posterior))
       (map (fn [x] (if (:theta-gt-pt07 x) 1 0))))
  :bins 100 :normalize :probability)
;; @@

;; **
;;; ### An alternative query: multiple observes
;;; 
;;; How would we modify this model to return, instead of a one-flip posterior, the posterior distribution given a sequence of flips? That is, we keep the basic model
;;; 
;;; $$\begin{align}\theta &\sim \mathrm{Beta}(5,3) \\\\
;;; x\_i &\sim \mathrm{Bernoulli}(\theta)\end{align}$$
;;; 
;;; and ask 
;;; 
;;; $$p(\theta>0.7 | x\_i)$$
;;; 
;;; for some sequence @@x\_i@@. Now, we let `outcomes`, the argument to our query, be a sequence, and we can use `map` (or `loop` and `recur`) to `observe` all different outcomes.
;;; 
;;; Here's one way of writing this:
;; **

;; @@
(defquery many-flips [outcomes]
  (let [theta (sample (beta 5 3))
        outcome-dist (flip theta)
        likelihood (fn [x] (observe outcome-dist x))]
    (map likelihood outcomes)
    (predict :theta-gt-pt07 (> theta 0.7))))
;; @@

;; **
;;; We can use `conditional` to estimate the posterior distribution of @@\theta > 0.7@@ given the sequence `[true, false, false, true]`, just as before (the analytical answer is 0.21).
;; **

;; @@
(def many-flip-posterior (conditional many-flips :lmh))


(frequencies 
  (repeatedly 1000 
              #(sample (many-flip-posterior [true false false true]))))
;; @@

;; **
;;; That's it! Now move onto the exercises. Keep this worksheet open in a separate tab or window, and refer to it for language reference.
;; **

;; **
;;; ## Advanced usage (not necessary for exercises)
;;; 
;;; If you are familiar with sampling techniques both in general and in Anglican you may directly interact with the sampler output that is hidden behing `conditional` syntactic sugar, bearing in mind that some samplers return _weighted samples_ which should be accounted for in subsequent use and some will return samples with weight `-Infinity` indicating that not all constraints have been satisfied yet; `conditional` does this and more under the covers
;;; 
;; **

;; @@
(def one-flip-sample
  (->> (doquery :lmh one-flip [true])
       (filter 
         #(> (:log-weight %)
             Double/NEGATIVE_INFINITY))
       (map get-predicts)
       (map :theta-gt-pt07)))
(frequencies (take 10000 one-flip-sample))
;; @@

;; **
;;; Otherwise you may use helper functions that collate sampler output in a way that properly uses the weights (for instance returned by the `:smc` sampler,  retaining statistical effeciency but requiring you to use the helper functions in `anglican.stat`
;; **

;; @@
(->> (doquery :smc one-flip [true] 
              :number-of-particles 100)
     (take 10000)
     (collect-by :theta-gt-pt07)
     (stat/empirical-distribution))

;; @@
