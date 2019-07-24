import React from 'react';
import { connect } from 'react-redux';
import MaterialIcon from '@material/react-material-icon';
import {
  searchEntities,
  setSearchTerm,
  insertSuggestions,
} from 'Actions/SearchActions';

export class SearchBar extends React.Component {
  clearInput = () => {
    this.props.setSearchTerm('');
    this.props.insertSuggestions([]);
  };

  handleChange = event => {
    const term = event.target.value;
    this.props.onSearchChange(term);
  };

  render() {
    return (
      <div className="searchBarContainer">
        <MaterialIcon icon="search" className="search" />
        <input
          className="searchBar"
          placeholder="Search for a table name"
          type="text"
          value={this.props.searchTerm}
          onChange={this.handleChange}
          // stop browsers from suggesting autocomplete text
          autoComplete="off"
        />
        <MaterialIcon
          icon="close"
          className="close"
          onClick={this.clearInput}
        />
      </div>
    );
  }
}

/* istanbul ignore next */ const mapStateToProps = ({ search }) => ({
  searchTerm: search.get('searchTerm'),
});

/* istanbul ignore next */ const mapDispatchToProps = dispatch => ({
  onSearchChange: searchTerm => dispatch(searchEntities(searchTerm)),
  setSearchTerm: term => dispatch(setSearchTerm(term)),
  insertSuggestions: suggestions => dispatch(insertSuggestions(suggestions)),
});

// exporting the connected component for the app
export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(SearchBar);
