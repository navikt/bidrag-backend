import Conf from "conf";

type ENV = "q1" | "q2";
const schema = {
  nais_cluster: {
    type: "string",
  },
};

export const config = new Conf({ projectName: "bidrag-cli", schema });
