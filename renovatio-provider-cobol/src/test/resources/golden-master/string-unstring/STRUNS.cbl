       IDENTIFICATION DIVISION.
       PROGRAM-ID. STRUNS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-NAME              PIC X(10) VALUE 'ANN SMITH'.
       01  WS-CITY              PIC X(08) VALUE 'PARIS'.
       01  WS-COUNT             PIC 9(03) VALUE 7.
       01  WS-MSG               PIC X(30) VALUE ALL '*'.
       01  WS-SHORT             PIC X(08) VALUE SPACES.
       01  WS-PTR               PIC 9(02) VALUE 1.
       01  WS-USER.
           05 USR-ID            PIC X(05) VALUE 'U0042'.
       01  WS-CSV               PIC X(30) VALUE 'AB,123,,XYZ,TAIL'.
       01  WS-WORDS             PIC X(20) VALUE 'ONE   TWO THREE'.
       01  WS-F1                PIC X(05) VALUE 'xxxxx'.
       01  WS-F2                PIC 9(04) VALUE 9999.
       01  WS-F3                PIC X(05) VALUE 'yyyyy'.
       01  WS-F4                PIC X(05) VALUE 'zzzzz'.
       01  WS-F5                PIC X(03) VALUE 'qqq'.
       PROCEDURE DIVISION.
       MAIN-PARA.
           STRING 'Hello ' DELIMITED BY SIZE
                  WS-NAME   DELIMITED BY SPACE
                  ', '      DELIMITED BY SIZE
                  WS-CITY   DELIMITED BY SPACE
                  '!'       DELIMITED BY SIZE
                  INTO WS-MSG
           END-STRING
           DISPLAY '[' WS-MSG ']'
           MOVE SPACES TO WS-MSG
           STRING 'Count=' WS-COUNT ' user='
                  USR-ID OF WS-USER
                  DELIMITED BY SIZE
                  INTO WS-MSG
           DISPLAY '[' WS-MSG ']'
           STRING 'ABCDEFGHIJKL' DELIMITED BY SIZE INTO WS-SHORT
           DISPLAY '[' WS-SHORT ']'
           MOVE SPACES TO WS-SHORT
           MOVE 3 TO WS-PTR
           STRING 'XY' DELIMITED BY SIZE
                  'Z-Q' DELIMITED BY '-'
                  INTO WS-SHORT WITH POINTER WS-PTR
           END-STRING
           DISPLAY '[' WS-SHORT '] ' WS-PTR
           UNSTRING WS-CSV DELIMITED BY ','
                INTO WS-F1 WS-F2 WS-F3 WS-F4
           END-UNSTRING
           DISPLAY '[' WS-F1 '][' WS-F2 '][' WS-F3 '][' WS-F4 ']'
           UNSTRING WS-WORDS DELIMITED BY ALL SPACE
                INTO WS-F1 WS-F3 WS-F4 WS-F5
           DISPLAY '[' WS-F1 '][' WS-F3 '][' WS-F4 '][' WS-F5 ']'
           GOBACK.
