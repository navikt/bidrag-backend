import { localeMap, SpråkType } from "./oversettelser";
import { DokumentmalPersonDto } from "~/types/Api";

export function sortByAge(
  rolleA: DokumentmalPersonDto,
  rolleB: DokumentmalPersonDto,
) {
  return (
    (dateOrNull(rolleA.fødselsdato)?.getTime() ?? 1) -
    (dateOrNull(rolleB.fødselsdato)?.getTime() ?? 1)
  );
}
export const dateOrNull = (dateString?: string | null): Date | null =>
  dateString ? new Date(dateString) : null;
export const toISODateString = (date?: Date): string | null =>
  date?.toLocaleDateString("sv-SV", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }) ?? null;
export const toISODateTimeString = (date?: Date): string | null =>
  date == undefined
    ? null
    : date?.toLocaleDateString("sv-SV", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      }) +
      "T" +
      date?.toLocaleTimeString() +
      "Z";
export const addDays = (date: Date, days: number) => {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + days);
  return newDate;
};

export const deductDays = (date?: Date | string | null, days: number = 1) => {
  const dateObject = date instanceof Date ? date : dateOrNull(date);
  if (dateObject == null) return null;
  const newDate = new Date(dateObject);
  newDate.setDate(newDate.getDate() - days);
  return newDate;
};

export const addMonths = (date: Date, months: number) => {
  const newDate = new Date(date);
  newDate.setMonth(newDate.getMonth() + months);
  return newDate;
};

export const deductMonths = (date: Date, months: number) => {
  const newDate = new Date(date);
  newDate.setMonth(newDate.getMonth() - months);
  return newDate;
};

export const lastDayOfMonth = (dateValue: Date | string): Date => {
  if (typeof dateValue == "string") {
    const dateObject = new Date(dateValue);
    return new Date(dateObject.getFullYear(), dateObject.getMonth() + 1, 0);
  } else {
    return new Date(dateValue.getFullYear(), dateValue.getMonth() + 1, 0);
  }
};
export const firstDayOfMonth = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth(), 1);
export const isValidDate = (date: unknown | Date): boolean =>
  !!(date && date instanceof Date && isFinite(date.getTime()));

export const toDateString = (date: Date) =>
  date.toLocaleDateString("no-NO", { dateStyle: "short" });

export const DDMMYYYYStringToDate = (dateString: string) => {
  const [day, month, year] = dateString.split(".").map((d, i) => {
    if (i === 1) return Number(d) - 1;
    if (i === 2) return Number(d);
    return Number(d);
  });

  return new Date(year, month, day);
};
export const DateToDDNameYYYYString = (date: Date) =>
  date?.toLocaleDateString("nb-NO", {
    year: "numeric",
    month: "long",
    day: "2-digit",
  });

export const DateToYearString = (date: Date) =>
  date?.toLocaleDateString("nb-NO", {
    year: "numeric",
  });
export const DateToDDMMYYYYString = (date: Date) =>
  date?.toLocaleDateString("nb-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

export const DateToDDMMYYYYHHMMString = (date: Date) =>
  date?.toLocaleDateString("nb-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });

export const DateToMMYYYYString = (date: Date) =>
  date.toLocaleDateString("nb-NO", { year: "numeric", month: "2-digit" });

export const ISODateTimeStringToDDMMYYYYString = (
  isoDateTimeString: string,
) => {
  const date = new Date(isoDateTimeString);
  return isValidDate(date) ? DateToDDMMYYYYString(date) : "";
};

export const isFirstDayOfMonth = (date: Date) => {
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
  return firstDay.getDate() === date.getDate();
};

export const isLastDayOfMonth = (date: Date) => {
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return lastDay.getDate() === date.getDate();
};

export const datesAreFromSameMonthAndYear = (date: Date, testDate: Date) =>
  date.getMonth() === testDate.getMonth() &&
  date.getFullYear() === testDate.getFullYear();

export const getAListOfMonthsFromDate = (
  fromDate: Date,
  numberOfMonths: number,
): Date[] => {
  const months = [];
  for (let i = 0; i <= numberOfMonths - 1; i++) {
    months.push(addMonths(firstDayOfMonth(fromDate), i));
  }
  return months;
};

export const periodCoversMinOneFullCalendarMonth = (
  startDate: Date,
  endDate: Date,
) => {
  const yearsDiff = endDate.getFullYear() - startDate.getFullYear();
  const monthDiff = endDate.getMonth() - startDate.getMonth();
  const endDateIsLastDayOfMonth = isLastDayOfMonth(endDate);

  if (yearsDiff === 0) {
    if (isFirstDayOfMonth(startDate) && endDateIsLastDayOfMonth) return true;

    return monthDiff >= (endDateIsLastDayOfMonth ? 1 : 2);
  }

  return endDateIsLastDayOfMonth || monthDiff + 12 > 1;
};

export const isAfterDate = (
  date: Date | string,
  comparedWith: Date | string = new Date(),
) => {
  const d = new Date(date);
  d.setHours(0, 0, 0);
  const c = new Date(comparedWith);
  c.setHours(0, 0, 0);

  return d.getTime() > c.getTime();
};
export const isAfterEqualsDate = (
  date?: Date | string,
  comparedWith: Date | string = new Date(),
) => {
  if (date == null) return true;
  const d = new Date(date);
  d.setHours(0, 0, 0);
  const c = new Date(comparedWith);
  c.setHours(0, 0, 0);

  return d.getTime() >= c.getTime();
};
export const isBeforeDate = (
  date: Date | string,
  comparedWith: Date | string = new Date(),
) => {
  const d = new Date(date);
  d.setHours(0, 0, 0);
  const c = new Date(comparedWith);
  c.setHours(0, 0, 0);

  return d.getTime() < c.getTime();
};

export const isBeforeOrEqualsDate = (
  date: Date | string,
  comparedWith: Date | string = new Date(),
) => {
  const d = new Date(date);
  d.setHours(0, 0, 0);
  const c = new Date(comparedWith);
  c.setHours(0, 0, 0);

  return d.getTime() <= c.getTime();
};
export const getYearFromDate = (date?: Date | string): number | undefined => {
  if (!date) return;
  if (typeof date == "string") return new Date(date).getFullYear();
  return date.getFullYear();
};
export const dateToDDMMYYYY = (
  date?: Date | string | null,
): string | undefined => {
  if (!date) return "";
  if (typeof date == "string") {
    if (isNaN(Date.parse(date))) return "";
    return new Date(date).toLocaleDateString("nb-NB", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  }

  return date.toLocaleDateString("nb-NB", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
};
export const formatPeriode = (
  dateFom: Date | string,
  dateTom: Date | string | undefined | null,
): string | undefined => {
  if (dateTom == undefined) return dateToDDMMYYYY(dateFom);
  return `${dateToDDMMYYYY(dateFom)} - ${dateToDDMMYYYY(lastDayOfMonth(dateTom))}`;
};

/** Formatter for år og måned i formatet "MMMM, ÅÅÅÅ" */
/**
 * @param årMånedStr - Input streng i formatet 'ÅÅÅÅ-MM'
 * @param språkKode - Språkkode for formatering (standard er 'nb')
 * @returns Formatert streng i formatet "MMMM, ÅÅÅÅ"
 * @example
 * formatterÅrMåned('2023-10', 'nb'); // Returneres "Oktober, 2023"
 * formatterÅrMåned('2023-10', 'en'); // Returneres "October, 2023"
 */
export function formatterÅrMåned(
  årMånedStr: string,
  språkKode: SpråkType = "nb",
): string {
  // Forventer input på formatet 'YYYY-MM'
  const locale = localeMap[språkKode] ?? "nb-NO";
  const [årstall, månedNr] = årMånedStr.split("-").map((v) => Number(v));

  const date = new Date(Date.UTC(årstall, månedNr - 1, 1));

  // Hent bare månedsnavn, så legger vi på år og komma selv
  const månedsNavn = new Intl.DateTimeFormat(locale, { month: "long" }).format(
    date,
  );

  return `${capitalize(månedsNavn)}, ${årstall}`;
}

const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
