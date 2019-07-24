const apiMock = {
  nodes: [
    {
      id: 1333832958231356438,
      group: 'table',
      label: 'SRCETL_COMBINED_AUDITINFO_DIM',
    },
    { id: -5164229199702592055, group: 'table', label: 'STGETL_AUDITINFO_DIM' },
    {
      id: -6257875581471897958,
      group: 'table',
      label: 'FACT_QBO_PRODUCT_ACTION_EVENT',
    },
    {
      id: 8393607891502255639,
      group: 'table',
      label: 'DIM_QBO_COMPANY_PRODUCT_USAGE_KPI',
    },
    { id: 4931414091262092880, group: 'table', label: 'DIM_QBO_USER' },
    {
      id: 5505949582699571067,
      group: 'table',
      label: 'QBO_COMPANY_PRODUCT_USAGE_KPI_VW',
    },
    { id: 2897333401185500543, group: 'table', label: 'CFG_IOP_ETL_STATE' },
  ],
  edges: [
    { from: 4931414091262092880, to: -5164229199702592055 },
    { from: -5164229199702592055, to: -6257875581471897958 },
    { from: -6257875581471897958, to: 2897333401185500543 },
    { from: -6257875581471897958, to: 8393607891502255639 },
    { from: 4931414091262092880, to: 1333832958231356438 },
    { from: 8393607891502255639, to: 5505949582699571067 },
    { from: 1333832958231356438, to: -6257875581471897958 },
  ],
};

export default apiMock;
