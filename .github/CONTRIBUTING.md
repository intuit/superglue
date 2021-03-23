# Contribution Guidelines

First of all, thanks for your interest in this project!

Depending on what changes you'd like to see, there may be different steps
for you to take, but in general:

#### Talk to us!

[![Join the chat at https://gitter.im/intuit/superglue](https://badges.gitter.im/intuit/superglue.svg)](https://gitter.im/intuit/superglue?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

If you're thinking of adding a feature or fixing a bug, make
sure you know that nobody's already working on it. Check if there's an open
issue, and if there isn't, feel free to make one! If we know what you're
trying to accomplish, chances are we can help you out or point you in the
right direction.

#### Read through what's already here

Consistency helps us understand each other better. If you write code in a
similar style to ours, we'll be able to review and approve it faster. If
you write documentation with the same voice, readers will be able to
transition smoothly between topics.

## Code changes

The first step in changing the code is running the code! In order to build
and run the code, you should have the following tools installed:

* JDK 8

#### Building the Backend

We use Scala 2.12 in our backend, and build it using Gradle. To build all the
components at once, open a command line to the project directory and run:

```
# For Unix/MacOS
./gradlew build

# For Windows
gradlew build
```

#### Development workflow

This project is written in Scala, so we recommend using IntelliJ with
the Scala plugin installed. Additionally, you should configure IntelliJ
to execute builds through gradle. Go to
`Preferences | Build, Execution, Deployment | Build Tools | Gradle | Runner`
and check the "Delegate IDE build/run actions to gradle" box. This allows
you to set up run configurations in IntelliJ that execute in debug mode,
which can help your development flow.

#### Checking coverage

We'd like to make sure our code is well-tested so users can be confident in it.
We can check code coverage by using `./gradlew reportScoverage`. This will
compile all the modules and generate a coverage report for each. Copy the
HTML link for the module you've edited to view the coverage report. To see if
the coverage meets our threshold requirements, run `./gradlew checkScoverage`,
which will only pass if coverage is high enough.

### Pull requests for Code

#### Branching and Rebasing

Pull requests for code changes should be branched from `master`, and should
be rebased against the latest `master` code. After merging a branch, the
history should be clean and shallow, as shown below:

```
*   e36331b - Merge branch 'issue1234' - (HEAD -> master)
|\
| * 047d6fd - #1234: Adds documentation - (issue1234)
| * 8bb93f7 - #1234: Adds regression tests
| * c61db8b - #1234: Fixes bug in parser
|/
* 89fd7b3 - Initial commit (master-before-merge)
```

> You can print a commit graph similar to this one using the command
> `git log --oneline --graph --all`

If your branch falls behind, you can bring it up-to-date by checking out your
branch and using `git rebase master`. Below you can see the difference between
a branch that's "fallen behind" versus one that's up-to-date:

```
* b47014b - #9876: Adds tests to api - (HEAD -> issue9876)
* bbc4d83 - #9876: Adds feature to api
*   e36331b - Merge branch 'issue1234' - (master)
|\
| * 047d6fd - #1234: Adds documentation - (issue1234)
| * 8bb93f7 - #1234: Adds regression tests
| * c61db8b - #1234: Fixes bug in parser
|/
| * 0a1f493 - #9876: Adds tests to api - (issue9876-fell-behind)
| * 3dcf143 - #9876: Adds feature to api
|/
* 89fd7b3 - Initial commit - (master-before-merge)
```

#### Don't commit secrets!

Make sure that you haven't accidentally committed any usernames, passwords,
or other sensitive information in the codebase. Secrets should always be
provided to the program through environment variables or configuration files
(_ones that don't get committed!_). See some of our [configuration docs] to
see how to handle these properly.

#### Squashing

If you've made a ton of commits in your branch, we may ask you to squash your
branch before we merge it (we could also do it for you :)). Squashing takes
all of the changes from all of your commits and "squashes" them into a single
new commit. This helps us maintain a clean and readable version history.

## Documentation changes

Our documentation is split into API documentation (scaladocs) and high-level
documentation (markdown). If you'd like to change the API documentation, just
treat it like a code change and make a branch off of `master`. If you want to
edit the high-level documentation, you'll need to make a branch from `gh-pages`.

#### API docs

Scaladoc provides some neat tools for making the generated API page nice and
readable. You can check out some scaladoc conventions [here], but we'll also
mention some we like to use:

[here]: https://docs.scala-lang.org/style/scaladoc.html

* Link to classes using `[[DoubleSquareBrackets]]`
* Use `@param` and `@return` tags to describe function inputs and outputs
* Use triple braces for code blocks:
  ```
  /** Show how to use a function in its doc.
    * {{{
    *   def isEven(n: Int): Boolean = n % 2 == 0
    *   assert(isEven(10))
    * }}}
    */
  ```

#### High-level documentation

Our high-level documentation is written using Github Pages, and is where people
can go to get an intuitive understanding of the problems the project solves and
the design of the code that solves those problems.

This documentation should be easy-to-read prose. It should be readable for people
who don't necessarily know the intricacies and pain points of the problem space,
but are curious to learn more about it. These docs should spend as much - if not
more time - describing the problems than the solutions (after all, the solutions
are free to change).

A good piece of documentation might read like the following:

> Modern large-scale applications often use a variety of data systems to store
> and analyze many different kinds of data. We often think of newly-collected
> records as "raw data", which are then refined by transforming and propagating
> the data through the system. This path through the system can be thought of
> as the data's "lineage". We would like an easy way to view and understand the
> lineage of our data in order to help maintain high data quality through fast
> debugging and error management.

Notice that this paragraph only talks about a problem statement, and doesn't
mention anything about how we plan to solve it yet. Writing clear problem
statements is the first step in helping to understand a solution.

### Pull requests for Documentation

Branches with changes to the high-level documentation should be branched from
`gh-pages`. The same rebasing practices are held here as with the code PRs.
Additionally, we recommend that you make doc changes on a _fork_ of this repo
using the same `gh-pages` branch name. This is so that when you push changes
to your fork, Github will automatically render the new docs at 
`github.com/pages/yourfork/superglue` and we can all see what the changes will
look like once they're merged.
