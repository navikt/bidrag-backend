import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {Persister} from "@tanstack/react-query-persist-client";
import {QueryClient} from "@tanstack/react-query";
import {PersistedClient} from "@tanstack/react-query-persist-client";
import {del, get, set} from "idb-keyval";
import {PersistQueryClientProvider} from "@tanstack/react-query-persist-client";
import {Suspense} from "react";
import {Loader} from "@navikt/ds-react";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 3,
      retryDelay: 2000,
      networkMode: "offlineFirst",
      gcTime: 1000 * 60 * 60 * 24, // 24 hours
      staleTime: 1000 * 60 * 5, // 5 minutes
    },
  },
});

/**
 * Creates an Indexed DB persister
 * @see https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API
 */
export function createIDBPersister(idbValidKey: IDBValidKey = "reactQuery") {
  return {
    persistClient: async (client: PersistedClient) => {
      await set(idbValidKey, client);
    },
    restoreClient: async () => {
      return await get<PersistedClient>(idbValidKey);
    },
    removeClient: async () => {
      await del(idbValidKey);
    },
  } as Persister;
}

createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <PersistQueryClientProvider
          client={queryClient}
          persistOptions={{persister: createIDBPersister("bidrag-admin-ui"), maxAge: Infinity}}
      >
        <Suspense
            fallback={
              <div className="flex justify-center">
                <Loader size="3xlarge" title="venter..." variant="interaction"/>
              </div>
            }
        >
          <App/>
        </Suspense>
      </PersistQueryClientProvider>
    </StrictMode>,
)
