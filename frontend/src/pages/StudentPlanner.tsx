import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Grid,
  Card,
  CardContent,
  CardActions,
  Divider,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  InputLabel,
  Select,
  FormControl,
  MenuItem,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import {
  useSections,
  useStudentSchedule,
  useAcademicProgress,
  useEnrollStudent,
} from '../hooks/useSchedule';
import ScheduleCalendar from '../components/ScheduleCalendar';
import { formatTimeRange, getDayName, semesterList } from '../utils/calendarUtils';
import type { Section } from '../types';
import type { AxiosError } from 'axios';

const StudentPlanner = () => {
  const { studentId: urlStudentId } = useParams();
  const [studentId, setStudentId] = useState<number>(Number(urlStudentId) || 0);
  const [semesterId, setSemesterId] = useState<number>(0);
  const [selectedSection, setSelectedSection] = useState<Section | null>(null);
  const [enrollDialogOpen, setEnrollDialogOpen] = useState(false);
  const [enrollmentStatus, setEnrollmentStatus] = useState<string>('');

  const { data: availableSections, isLoading: sectionsLoading } = useSections(semesterId, 'SCHEDULED');
  const { data: studentSchedule, isLoading: scheduleLoading } = useStudentSchedule(studentId, semesterId);
  const { data: progress } = useAcademicProgress(studentId);
  const enrollMutation = useEnrollStudent();

  const handleEnrollClick = (section: Section) => {
    setSelectedSection(section);
    setEnrollDialogOpen(true);
  };

  const handleEnrollConfirm = async () => {
    if (!selectedSection) return;

    try {
      await enrollMutation.mutateAsync({
        studentId,
        sectionId: selectedSection.id,
      });
    } catch (error) {
      console.error('Enrollment failed:', error);
    }
  };

  const isSectionFull = (section: Section) => {
    return section.enrolledCount >= section.capacity;
  };

  const enrolledSections = useMemo(() => studentSchedule?.sections, [studentSchedule]);

  const isEnrolled = (sectionId: number) => {
    return enrolledSections?.some((s) => s.id === sectionId);
  };

  useEffect(() => {
    if (enrollMutation.isSuccess) {
      setEnrollmentStatus('success');
    } else if (enrollMutation.isError) {
      setEnrollmentStatus('error');
    }
  }, [enrollMutation.isSuccess, enrollMutation.isError]);

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Student Course Planner
      </Typography>

      <Grid container spacing={3}>
        {/* Student Info & Controls */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper elevation={2} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Student Information
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <TextField
              label="Student ID"
              type="text"
              value={studentId}
              onChange={(e) => setStudentId(Number(e.target.value))}
              fullWidth
              margin="normal"
            />

            <FormControl fullWidth sx={{ mt: 2 }}>
              <InputLabel id="semester-id-label">Semester Id</InputLabel>
              <Select
                labelId="semester-id-label"
                id="semester-id-select"
                value={semesterId}
                label="Semester Id"
                onChange={(e) => setSemesterId(Number(e.target.value))}
              >
                {
                  semesterList.map(sem => <MenuItem value={sem.value}>{sem.label}</MenuItem>)
                }
              </Select>
            </FormControl>

            {progress && (
              <Card sx={{ mt: 2 }}>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    {progress.studentName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Grade Level: {progress.gradeLevel}
                  </Typography>
                  <Divider sx={{ my: 1 }} />
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                    <Typography variant="body2">Completed:</Typography>
                    <Chip label={progress.coursesCompleted || 0} size="small" color="success" />
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                    <Typography variant="body2">Enrolled:</Typography>
                    <Chip label={(enrolledSections && enrolledSections.length) ?? 0} size="small" color="primary" />
                  </Box>
                </CardContent>
              </Card>
            )}
          </Paper>

          {/* Enrolled Courses */}
          <Paper elevation={2} sx={{ p: 3, mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              My Schedule
            </Typography>
            <Divider sx={{ mb: 2 }} />

            {scheduleLoading && <CircularProgress />}

            {!scheduleLoading && enrolledSections && enrolledSections?.length > 0 && (
              <List>
                {enrolledSections.map((section) => (
                  <ListItem key={section.id} sx={{ px: 0 }}>
                    <ListItemText
                      primary={`${section.courseName} (${section.sectionNumber})`}
                      secondary={
                        <>
                          <Typography variant="body2" component="span">
                            {section.teacherName || 'TBA'}
                          </Typography>
                          <br />
                          <Typography variant="caption" component="span">
                            {section.hoursPerWeek}h/week
                          </Typography>
                        </>
                      }
                    />
                    <CheckCircleIcon color="success" />
                  </ListItem>
                ))}
              </List>
            )}

            {!scheduleLoading && enrolledSections && enrolledSections?.length === 0 && (
              <Alert severity="info">No enrolled courses yet.</Alert>
            )}
          </Paper>
        </Grid>

        {/* Available Sections & Calendar */}
        <Grid size={{ xs: 12, md: 8 }}>
          {/* Personal Calendar */}
          {enrolledSections && enrolledSections?.length > 0 && (
            <Box sx={{ mb: 3 }}>
              <ScheduleCalendar
                sections={enrolledSections}
                semesterStartDate={studentSchedule?.semesterStartDate as string}
                semesterEndDate={studentSchedule?.semesterEndDate as string}
                title="My Personal Schedule"
                height={500}
              />
            </Box>
          )}

          {/* Available Sections */}
          <Paper elevation={2} sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Available Courses</Typography>
              <Chip label={`${availableSections?.length || 0} sections`} size="small" />
            </Box>
            <Divider sx={{ mb: 2 }} />

            {sectionsLoading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
            )}

            {!sectionsLoading && availableSections && availableSections.length > 0 && (
              <Grid container spacing={2}>
                {availableSections.map((section) => {
                  const enrolled = isEnrolled(section.id);
                  const full = isSectionFull(section);

                  return (
                    <Grid size={{ xs: 12, sm: 6 }} key={section.id}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="h6" gutterBottom>
                            {section.courseName}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" gutterBottom>
                            Section {section.sectionNumber}
                          </Typography>

                          <Box sx={{ mt: 1, mb: 1 }}>
                            {/* <Chip
                              label={`${section.enrolledCount}/${section.capacity}`}
                              size="small"
                              color={full ? 'error' : 'default'}
                              sx={{ mr: 1 }}
                            /> */}
                            <Chip label={`${section.timeslots?.length}h/week`} size="small" />
                          </Box>

                          <Typography variant="body2" sx={{ mt: 1 }}>
                            👨‍🏫 {section.teacherName || 'TBA'}
                          </Typography>
                          <Typography variant="body2">
                            🏫 {section.classroomName || 'TBA'}
                          </Typography>

                          <Divider sx={{ my: 1 }} />

                          <Typography variant="caption" display="block" gutterBottom>
                            Schedule:
                          </Typography>
                          {section.timeslots.map((ts) => (
                            <Typography key={ts.id} variant="caption" display="block">
                              {getDayName(ts.dayOfWeek)}: {formatTimeRange(ts.startTime, ts.endTime)}
                            </Typography>
                          ))}
                        </CardContent>

                        <CardActions>
                          {enrolled ? (
                            <Button
                              size="small"
                              startIcon={<CheckCircleIcon />}
                              disabled
                              fullWidth
                              variant="outlined"
                            >
                              Enrolled
                            </Button>
                          ) : (
                            <Button
                              size="small"
                              variant="contained"
                              startIcon={<AddIcon />}
                              onClick={() => handleEnrollClick(section)}
                              disabled={full}
                              fullWidth
                            >
                              {full ? 'Full' : 'Enroll'}
                            </Button>
                          )}
                        </CardActions>
                      </Card>
                    </Grid>
                  );
                })}
              </Grid>
            )}

            {!sectionsLoading && (!availableSections || availableSections.length === 0) && (
              <Alert severity="info">
                No available sections for semester {semesterId}.
              </Alert>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Enrollment Confirmation Dialog */}
      <Dialog open={enrollDialogOpen} onClose={() => setEnrollDialogOpen(false)}>
        <DialogTitle>Confirm Enrollment</DialogTitle>
        <DialogContent>
          {selectedSection && (
            <Box>
              <Typography variant="h6" gutterBottom>
                {selectedSection.courseName}
              </Typography>
              <Typography variant="body2" gutterBottom>
                Section {selectedSection.sectionNumber}
              </Typography>
              <Typography variant="body2" gutterBottom>
                Teacher: {selectedSection.teacherName || 'TBA'}
              </Typography>
              <Typography variant="body2" gutterBottom>
                Hours per week: {selectedSection.hoursPerWeek}
              </Typography>

              <Divider sx={{ my: 2 }} />

              <Typography variant="body2" gutterBottom>
                Schedule:
              </Typography>
              {selectedSection.timeslots.map((ts) => (
                <Typography key={ts.id} variant="body2">
                  {getDayName(ts.dayOfWeek)}: {formatTimeRange(ts.startTime, ts.endTime)}
                </Typography>
              ))}

              {enrollmentStatus === 'error' && (
                <Alert severity="error" sx={{ mt: 2 }} >
                  {(enrollMutation.error as AxiosError<{ message: string }>)?.response?.data?.message}
                </Alert>
              )}

              {enrollmentStatus === 'success' && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  Successfully enrolled!
                </Alert>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setEnrollDialogOpen(false)
            setEnrollmentStatus('')
          }}>Cancel</Button>
          <Button
            onClick={handleEnrollConfirm}
            variant="contained"
            disabled={enrollMutation.isPending  || enrollmentStatus === 'success' || studentId < 1}
            startIcon={enrollMutation.isPending ? <CircularProgress size={20} /> : <AddIcon />}
          >
            {enrollMutation.isPending ? 'Enrolling...' : 'Confirm Enrollment'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default StudentPlanner;

