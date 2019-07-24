import axios from 'axios';

export default axios.create({
  baseURL: `${process.env.GRAPH_HOST}/api/v1/`,
});
