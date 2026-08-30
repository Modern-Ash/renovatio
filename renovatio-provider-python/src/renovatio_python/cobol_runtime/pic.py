"""COBOL PICTURE clause parsing -> PicType.

Behavioural mirror of ``org.shark.renovatio.cobol.runtime.PicClause`` /
``PicType`` in the Java module ``renovatio-cobol-runtime``. Keep the two in sync.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum


class Category(Enum):
    NUMERIC = "NUMERIC"
    ALPHANUMERIC = "ALPHANUMERIC"
    ALPHABETIC = "ALPHABETIC"


class Usage(Enum):
    DISPLAY = "DISPLAY"
    COMP = "COMP"
    COMP_3 = "COMP_3"
    COMP_5 = "COMP_5"


@dataclass(frozen=True)
class PicType:
    category: Category
    digits: int
    scale: int
    signed: bool
    usage: Usage

    @property
    def integer_digits(self) -> int:
        return self.digits - self.scale


class PicClause:
    @staticmethod
    def parse(raw: str) -> PicType:
        if raw is None or not raw.strip():
            raise ValueError("empty picture clause")

        clause = raw.strip().upper()
        clause = re.sub(r"^PICTURE\b", "PIC", clause)
        clause = re.sub(r"^PIC\b", "", clause)
        clause = re.sub(r"^\s*IS\b", "", clause).strip()

        usage = PicClause._detect_usage(clause)
        sign_pattern = (
            r"\bSIGN(?:\s+IS)?\s+(?:LEADING|TRAILING)"
            r"(?:\s+SEPARATE(?:\s+CHARACTER)?)?\b"
        )
        separate_sign = re.search(sign_pattern, clause) is not None

        picture = re.sub(
            r"\b(?:PACKED-DECIMAL|COMPUTATIONAL-3|COMPUTATIONAL-5|"
            r"COMPUTATIONAL-4|COMPUTATIONAL|COMP-3|COMP-5|COMP-4|"
            r"COMP|BINARY|DISPLAY|USAGE)\b",
            " ",
            clause,
        )
        picture = re.sub(sign_pattern, " ", picture).strip()
        symbols = PicClause._expand_repeats(picture)

        signed = separate_sign or symbols.startswith("S")
        if symbols.startswith("S"):
            symbols = symbols[1:]

        if "X" in symbols:
            return PicType(Category.ALPHANUMERIC, symbols.count("X"), 0, False, usage)
        if "A" in symbols:
            return PicType(Category.ALPHABETIC, symbols.count("A"), 0, False, usage)

        digits = symbols.count("9")
        if digits == 0:
            raise ValueError(f"invalid picture clause: {raw}")
        v_index = symbols.find("V")
        scale = 0 if v_index < 0 else symbols[v_index + 1 :].count("9")
        return PicType(Category.NUMERIC, digits, scale, signed, usage)

    @staticmethod
    def _detect_usage(clause: str) -> Usage:
        if re.search(r"\b(?:COMP-3|COMPUTATIONAL-3|PACKED-DECIMAL)\b", clause):
            return Usage.COMP_3
        if re.search(r"\b(?:COMP-5|COMPUTATIONAL-5)\b", clause):
            return Usage.COMP_5
        if re.search(r"\b(?:COMP|COMP-4|COMPUTATIONAL|COMPUTATIONAL-4|BINARY)\b", clause):
            return Usage.COMP
        return Usage.DISPLAY

    @staticmethod
    def _expand_repeats(picture: str) -> str:
        def expand(match: re.Match[str]) -> str:
            count = int(match.group(2))
            if count > 1_000_000:
                raise ValueError("picture repetition is too large")
            return match.group(1) * count

        expanded = re.sub(r"([9XAP])\((\d+)\)", expand, picture)
        return re.sub(r"[^SVXAP9]", "", expanded)
