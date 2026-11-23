import { DokumentmalPersonDto, BeregnetBidragBarnDto } from "~/types/Api";

export interface BeregningBarn {
  beregnetBidragPerBarn: BeregnetBidragBarnDto;
  barn: DokumentmalPersonDto;
  bidragTilFordeling: number;
  erSøknadsbarn: boolean;
  privatAvtale: boolean;
  erBidragIkkeTilFordeling: boolean;
  utenlandskbidrag: boolean;
  oppfostringsbidrag: boolean;
}
