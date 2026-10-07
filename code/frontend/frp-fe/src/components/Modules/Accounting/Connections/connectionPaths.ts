export const TAB_PARAM = 'tab'
export const RUNS_TAB = 'runs'

/** Path of the connection detail relative to the connections list, optionally opening one of its tabs. */
export const connectionDetailPath = (connectionId: number, tab?: string) =>
  tab ? `${connectionId}?${TAB_PARAM}=${tab}` : String(connectionId)
