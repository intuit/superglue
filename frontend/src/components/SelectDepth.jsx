import React from 'react';
import { getLineageData } from '../actions/LineageActions';


export class SelectDepth extends React.Component {
  constructor(props){
    super(props);
      this.state = { value: '1'};
    }

  handleChange = event => {
    this.setState({value: event.target.value});
    const depth = event.target.value;
    this.props.getLineageData(this.props.entityName, this.props.entityType, depth);
  }

  render() {
    return (
        <div className="selectDepthContainer">
          <label htmlFor="depth">With a depth of: </label>
          <select className="depthTraversal" id="depthTraversal" onChange={this.handleChange}>
            <option value="1">1</option>
            <option value="2">2</option>
            <option value="3">3</option>
            <option value="4">4</option>
            <option value="Full">Full</option>
          </select>
        </div>
    )
  }
};


// const mapState = (state) => ({
//   depth: state.value,
// });
// const mapDispatch = (dispatch, { history }) => {
//   return {
//     getSingleProduct: (id) => dispatch(fetchSingleProduct(id)),
//     addProduct: (id, quantity, price, product) => dispatch(fetchAddProduct(id, quantity, price, product))
//   };
// };
// export default connect(mapState, mapDispatch)(SelectDepth);

export default SelectDepth;