import { styled } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell, { tableCellClasses } from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import type { CalendarEvent } from '../types';

const StyledTableCell = styled(TableCell)(({ theme }) => ({
  [`&.${tableCellClasses.head}`]: {
    backgroundColor: theme.palette.common.black,
    color: theme.palette.common.white,
  },
  [`&.${tableCellClasses.body}`]: {
    fontSize: 14,
  },
}));

const StyledTableRow = styled(TableRow)(({ theme }) => ({
  '&:nth-of-type(odd)': {
    backgroundColor: theme.palette.action.hover,
  },
  // hide last border
  '&:last-child td, &:last-child th': {
    border: 0,
  },
}));

interface eventType {
  event: CalendarEvent[]
}

const TablePopup = ({event}: eventType) => {
  function createData(
    classroom: string,
    courseCode: string,
    courseName: string,
    sectionNumber: number,
    teacherName: string
  ) {
    return { classroom, courseCode, courseName, sectionNumber, teacherName };
  }

  const rows = event.map(eve => createData(
    eve.classroomName as string, eve.courseCode, eve.courseName, eve.sectionNumber, eve.teacherName as string
  ));
    
  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      <TableContainer sx={{ maxHeight:440, width: '100%', overflowY: 'auto' }} component={Paper}>
        <Table stickyHeader aria-label="sticky table" sx={{ minWidth: 700 }}>
            <TableHead>
              <TableRow>
                  <StyledTableCell align="center">Classroom</StyledTableCell>
                  <StyledTableCell align="center">Course Code</StyledTableCell>
                  <StyledTableCell align="center">Course</StyledTableCell>
                  <StyledTableCell align="center">Section No.</StyledTableCell>
                  <StyledTableCell align="center">Teacher</StyledTableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                  <StyledTableRow key={row.classroom}>
                    <StyledTableCell component="th" scope="row" align="center">
                        {row.classroom}
                    </StyledTableCell>
                    <StyledTableCell align="center">{row.courseCode}</StyledTableCell>
                    <StyledTableCell align="center">{row.courseName}</StyledTableCell>
                    <StyledTableCell align="center">{row.sectionNumber}</StyledTableCell>
                    <StyledTableCell align="center">{row.teacherName}</StyledTableCell>
                  </StyledTableRow>
            ))}
            </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
}

export default TablePopup;