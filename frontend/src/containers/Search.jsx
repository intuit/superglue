import React from 'react';
import SearchBar from 'Components/SearchBar';
import SearchTable from 'Components/SearchTable';

const Search = () => (
  <div className="searchContainer">
    <div className="searchTopContainer">
      <h1 className="brand">superglue</h1>
      <SearchBar />
    </div>
    <SearchTable />
  </div>
);

export default Search;
