import LineageActionTypes from 'Constants/LineageActionTypes';
import LoadingState from 'Constants/LoadingState';
import API from './AxiosInstance';

export const setLineageLoading = state => ({
  type: LineageActionTypes.SET_LINEAGE_LOADING,
  state,
});

export const setLineageData = data => ({
  type: LineageActionTypes.INSERT_LINEAGE,
  data,
});

export const getLineageData = (entityName, entityType) => dispatch => {
  dispatch(setLineageLoading(LoadingState.LOADING));
  return API.get(`lineage/${entityType}/${entityName}`)
    .then(res => {
      dispatch(setLineageData(res.data));
      dispatch(setLineageLoading(LoadingState.FINISHED_SUCCESS));
    })
    .catch(err => {
      /* istanbul ignore next */ console.error(err);
      dispatch(setLineageLoading(LoadingState.FINISHED_FAILURE));
    });
};
