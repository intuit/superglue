# Superglue

[![Join the chat at https://gitter.im/intuit/superglue](https://badges.gitter.im/intuit/superglue.svg)](https://gitter.im/intuit/superglue?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Superglue is a lineage-tracking tool to help visualize
the propagation of data through complex pipelines.

![Superglue demo](.github/assets/SuperglueDemo.gif)

## Quick Start

Dependencies:

- JDK 8
- Docker

The first-time setup takes about five minutes.

![Superglue setup](.github/assets/SuperglueSetup.gif)

> **Note**: The gifs show superglue being hosted at `http://localhost:3000`,
> but that has since changed. Be sure to use `http://localhost:8080` instead!

Detailed instructions below!

### Launch the development environment with Docker

We've included a docker configuration to set up all of the services that
superglue needs to run. To launch the development image, run

```
docker-compose -f deployments/development/docker-compose.yml up
```

This launches

- A MySQL database on port `3314`
- The superglue frontend at `http://localhost:8080`
- The superglue backend at `http://localhost:8080/api`
- An elasticsearch server at `http://localhost:8080/elasticsearch`

> **Note**: By default, docker allocates 2GB of memory for containers, but you
> may need to increase this limit, otherwise elasticsearch will shut down.

### Install the command-line client

To install the superglue command-line client, run

```
./gradlew installDist
```

This will put the superglue executable into `~/.superglue/bin/`.
Add this directory to your path to use it as a command by pasting the following
line to the end of your `~/.bashrc`:

```
export PATH="${HOME}/.superglue/bin:${PATH}"
```

### Get started with sample data

We've included a sample SQL script with some dummy statements to illustrate
Superglue's usefulness. The next steps will assume you successfully installed
the `superglue` command-line tool and have the docker development containers
running.

The first thing we need to do is initialize the database. To do this, we need a
configuration file with the database's location and credentials. We've provided
one for this exercise in `examples/superglue.conf`.

```
cd examples
superglue init --database
```

> **Note**: The `superglue` tool automatically searches for a file called
> `superglue.conf` in the current directory to use as its configuration.

Next, we need to parse our sample data (in `examples/demo.sql`) and get it into
the database. Our configuration file also lists the files that should be parsed,
and again, the command-line tool will automatically use `superglue.conf`.

```
# In examples/
superglue parse
```

If everything works out, `superglue` should print out a json blurb that describes
the data it parsed, then it will pause for a few seconds as it inserts the data
into the database.

The last setup step is to load our data into elasticsearch so that we'll be able
to search through the data from the UI.

```
superglue elastic --load
```

Once all of that's done, head on over to a browser and open up `http://localhost:8080`.
You should be able to start searching for table names, and click one to see it's
lineage.

> **Note**: The sample data tables are named using _Lorem Ipsum_, so try searching one
> of those words.

### Tests

To run all of the tests, run:

```
./gradlew test
```

To check the code's test coverage, run:

```
# To just generate a report
./gradlew reportScoverage

# To pass or fail based on coverage threshold (75%)
./gradlew checkScoverage
```

After running `reportScoverage` (and also `checkScoverage` if it passed), you can
view the coverage report by opening a module's `build/reports/scoverage/index.html`
file in a browser.

## Contributing

If you'd like to contribute to Superglue, be sure to check out our
[contributing guidelines] and feel free to open an issue or pull request!

[contributing guidelines]: .github/CONTRIBUTING.md
