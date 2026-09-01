const cobolTerms = {
  COPYBOOK: 'Reusable COBOL data division fragment, similar to #include in C',
  PARAGRAPH: 'A named block of COBOL statements within a section',
  SECTION: 'A group of related paragraphs',
  DIVISION: 'Major structural unit (IDENTIFICATION, ENVIRONMENT, DATA, PROCEDURE)',
  FD: 'File Description entry, defines file structure',
  'WORKING-STORAGE': 'Variable declaration area in COBOL',
  PIC: 'Picture clause, defines data type and size',
  REDEFINES: 'Allows same memory to be interpreted as different data types',
  OCCURS: 'Defines arrays or repeated items',
  THRU: 'Keyword used in PERFORM...THRU to execute a range of paragraphs'
};

function CobolTooltip({ term, children }) {
  const definition = cobolTerms[term]
  
  if (!definition) {
    return children
  }

  return (
    <span className="relative group inline-block">
      {children}
      <span className="invisible group-hover:visible absolute z-10 w-64 p-2 mt-1 text-sm text-white bg-gray-900 rounded-lg shadow-lg">
        <span className="font-semibold">{term}:</span> {definition}
      </span>
    </span>
  )
}

export default CobolTooltip
export { cobolTerms }
