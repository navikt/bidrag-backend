import NavLogo from "~/components/NavLogo";
import { useVedtakFelles } from "~/components/vedtak_felles/VedtakContext";
import { Adresse, EnhetKontaktInfo } from "~/types/Api";

export default function BrevAdresseKontaktinfo() {
  const { data } = useVedtakFelles();

  const mottaker = data.mottaker;
  return (
    <div className={"soknad_detaljer"}>
      <div>
        <NavLogo />
        <div className={"flex flex-row justify-between"}>
          <div>
            <span>{mottaker?.navn}</span>
            {renderAdresse(mottaker?.adresse)}
          </div>
          {renderKontaktinfo(data?.kontaktInfo)}
        </div>
      </div>
    </div>
  );
}

function renderKontaktinfo(kontaktInfo?: EnhetKontaktInfo) {
  if (!kontaktInfo) return null;
  return (
    <div className={"flex flex-col"}>
      {kontaktInfo?.navn && <span>{kontaktInfo?.navn}</span>}
      {renderAdresse(kontaktInfo.postadresse)}
    </div>
  );
}
function renderAdresse(adresse?: Adresse) {
  if (!adresse) return null;
  const adresseFull =
    adresse.adresselinje1 +
    adresse.adresselinje2 +
    adresse.adresselinje3 +
    adresse.adresselinje4;
  const adresseInneholdPoststed =
    adresseFull && adresse?.poststed && adresseFull.includes(adresse?.poststed);
  return (
    <div className={"flex flex-col"}>
      <span>
        {adresse?.bruksenhetsnummer
          ? `Bolignummer ${adresse?.bruksenhetsnummer}`
          : ""}
      </span>
      {adresse?.adresselinje1 && <span>{adresse?.adresselinje1}</span>}
      {adresse?.adresselinje2 && <span>{adresse?.adresselinje2}</span>}
      {adresse?.adresselinje3 && <span>{adresse?.adresselinje3}</span>}
      {adresse?.adresselinje4 && <span>{adresse?.adresselinje4}</span>}
      {adresse?.postnummer && !adresseInneholdPoststed && (
        <span>{adresse?.postnummer + " " + adresse?.poststed}</span>
      )}
    </div>
  );
}
