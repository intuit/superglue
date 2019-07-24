import elasticsearch from 'elasticsearch';
import SearchActionTypes from 'Constants/SearchActionTypes';

const log = process.env.NODE_ENV === 'production' ? '' : 'trace';
const searchClient = new elasticsearch.Client({
  host: process.env.ELASTICSEARCH_HOST,
  log,
});

export const setSearchTerm = searchTerm => ({
  type: SearchActionTypes.SET_SEARCH_TERM,
  searchTerm,
});

export const insertSuggestions = suggestions => ({
  type: SearchActionTypes.INSERT_SUGGESTIONS,
  suggestions,
});

export const searchEntities = searchTerm => dispatch => {
  dispatch(setSearchTerm(searchTerm));
  return searchClient
    .search({
      index: 'lineage',
      body: {
        size: 50,
        query: {
          bool: {
            must: [
              {
                match: {
                  name: {
                    query: searchTerm,
                    // this part makes sure all the keywords are present
                    operator: 'and',
                    fuzziness: 1,
                  },
                },
              },
            ],
            should: [
              {
                term: {
                  name: {
                    // this also boosts the exact match results to the top
                    value: searchTerm,
                    boost: 20,
                  },
                },
              },
            ],
          },
        },
      },
    })
    .then(
      result => {
        const suggestions = result.hits.hits.map(item => ({
          name: item._source.name,
          type: item._source.type,
          system: item._source.platform,
          job_group: item._source.schema,
        }));

        // When we receive the suggestions, insert them into the store.
        dispatch(insertSuggestions(suggestions));
      },
      error => {
        if (process.env.NODE_ENV === 'development') {
          /* istanbul ignore next */ console.error('Elasticsearch error:');
          /* istanbul ignore next */ console.error(error);
        }
      },
    );
};
