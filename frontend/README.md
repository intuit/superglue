# Locally serve the project

To start the dev server, run
```
yarn start:dev
```
and go to `localhost:3000`. You can change the port in the file
`config/webpack-dev-server.js`.

You will be able to see the output of the compiler, but if you wish to turn it
off, you can set `noInfo: true` in the `webpack-dev-server.js` file.

To run the production build code locally, run
```
yarn start:prod
```
This one will be at `localhost:5000`.

## environment files

We employ environment files to manage the different urls and secrets. For development, create a file `config/.env.dev`
and follow this template to suit your needs:

```
GRAPH_HOST=url to the backend service (e.g. http://localhost:9000)
ELASTICSEARCH_HOST=host for the elasticsearch service (e.g. localhost)
ELASTICSEARCH_PROTOCOL=http or https
ELASTICSEARCH_PORT=port # for the search service (e.g. 9200)
```

# Build the project for production
```
yarn build
```

Then in the `build/` directory, you will see
- `hash_vendor.js`
- `hash_app.js`
- `index.html`
- `styles.css`
 
# To lint the project
The project is equipped with ESLint, Prettier, and Airbnb. To lint the project,
run
```
yarn run lint
```

To fix the errors you see after running the command, run the command again with 
```
yarn run lint --fix
```

To lint the Sass files, run
```
yarn run sass-lint
```

## Git hooks
When you commit the linter runs automatically. If you want to skip this step,
add `--no-verify` when you commit.
```
git commit -m "no lint please!" --no-verify
```

## Linter configs
All of the linters are configurable, and their settings are in these files
- `.eslintrc.json`
- `.eslintignore`
- `.prettierrc`
- `sass-lint.yml`

# To test the project
The project is using Jest and Enzyme to for the tests. To run them,
```
yarn test
```

To clear the cache of Jest
```
yarn test:clear
```

To automatically run tests when it sees changes in a certain file or directory,
```
yarn test:watch
```

To see the coverage report,
```
yarn test:coverage
```
