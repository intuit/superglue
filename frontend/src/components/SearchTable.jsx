import React from 'react';
import { connect } from 'react-redux';
import ReactTable from 'react-table';
import { withRouter } from 'react-router';
import 'react-table/react-table.css';

const columns = [
  {
    Header: 'Schema',
    accessor: 'schema',
    width: 80,
  },
  {
    Header: 'Name',
    accessor: 'name',
  },
  {
    Header: 'Type',
    id: 'type',
    accessor: /* istanbul ignore next */ d => d.type.toLowerCase(),
    width: 80,
  },
  {
    Header: 'Platform',
    accessor: 'platform',
    width: 80,
  },
];

/* istanbul ignore next */ const getLink = suggestion => {
  return `/dashboard/table/${suggestion.name}/`;
};

export const SearchTable = props => (
  <div className="searchTableContainer">
    <ReactTable
      data={props.suggestions}
      columns={columns}
      showPageSizeOptions={false}
      sortable={false}
      defaultPageSize={16}
      showPageJump={false}
      previousText="Back"
      pageText=""
      getTrProps={(state, rowInfo, column, instance) => ({
        onClick: /* istanbul ignore next */ e =>
          props.history.push(`${getLink(rowInfo.original)}`),
      })}
    />
  </div>
);

/* istanbul ignore next */ const mapStateToProps = ({ search }) => ({
  suggestions: search.get('suggestions'),
});

/* istanbul ignore next */ const mapDispatchToProps = dispatch => ({});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(withRouter(SearchTable));
