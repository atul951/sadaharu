import { useEffect, useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Grid,
  Divider,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useGenerateScheduleV1, useSchedule } from '../hooks/useSchedule';
import ScheduleCalendar from '../components/ScheduleCalendar';
import { semesterList } from '../utils/calendarUtils';
import type { ScheduleResponse } from '../types';

const AdminDashboard = () => {
  const [semesterId, setSemesterId] = useState<number>();
  const [semesterName, setSemesterName] = useState<string>();
  const [refreshSchedule, setRefreshSchedule] = useState<boolean>(false);

  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);

  const generateMutation = useGenerateScheduleV1();
  const { data: scheduleData, isLoading: scheduleLoading, refetch: refetchSchedule } = useSchedule(
    refreshSchedule && typeof semesterId !== 'undefined' ? semesterId : 0
  );

  const handleGenerate = async () => {
    try {
      if (typeof semesterId !== 'undefined') {
        const response = await generateMutation.mutateAsync({
          semesterId,
        });
        setSchedule(response);
      }
    } catch (error) {
      console.error('Failed to generate schedule:', error);
    }
  };

  useEffect(() => {
    setSchedule(scheduleData as ScheduleResponse);
  }, [scheduleData])

  const handleRefresh = () => {
    if(!refreshSchedule) {
      setRefreshSchedule(true);
    } else {
      refetchSchedule();
    }
  };

  const handleSemesterChange = (event: any) => {
    setRefreshSchedule(false);
    setSemesterId(Number(event.target.value));
    setSemesterName(semesterList.find(sem => sem.value === Number(event.target.value))?.label);
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Admin Dashboard - Schedule Generation
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 3 }}>
          <Paper elevation={2} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Generate Schedule
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <FormControl fullWidth>
              <InputLabel id="semester-id-label">Semester Id</InputLabel>
              <Select
                labelId="semester-id-label"
                id="semester-id-select"
                value={semesterId}
                label="Semester Id"
                onChange={handleSemesterChange}
              >
                {
                  semesterList.map(sem => <MenuItem value={sem.value}>{sem.label}</MenuItem>)
                }
              </Select>
            </FormControl>

            <Button
              variant="contained"
              color="primary"
              fullWidth
              startIcon={generateMutation.isPending ? <CircularProgress size={20} /> : <PlayArrowIcon />}
              onClick={handleGenerate}
              disabled={generateMutation.isPending || typeof semesterId === 'undefined'}
              sx={{ mt: 2 }}
            >
              {generateMutation.isPending ? 'Generating...' : 'Generate Schedule'}
            </Button>

            {generateMutation.isSuccess && (
              <Alert severity="success" sx={{ mt: 2 }}>
                Schedule generated successfully!
                <br />
                Scheduled: {generateMutation.data.sectionsScheduled} / {generateMutation.data.sectionsCreated}
                <br />
                Success Rate: {(generateMutation.data.statistics.success_rate * 100).toFixed(1)}%
              </Alert>
            )}

            {generateMutation.isError && (
              <Alert severity="error" sx={{ mt: 2 }}>
                Failed to generate schedule. Please try again.
              </Alert>
            )}
          </Paper>
        </Grid> 

        {/* Schedule View */}
        <Grid size={{ xs: 12, md: 9 }}>
          <Paper elevation={2} sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">
                Master Schedule {semesterName ? `- ${semesterName}` : '' } {semesterId ? `(Semester ${semesterId})` : ''}
              </Typography>
              <Button
                startIcon={<RefreshIcon />}
                onClick={handleRefresh}
                disabled={scheduleLoading}
              >
                Refresh
              </Button>
            </Box>
            <Divider sx={{ mb: 2 }} />

            {scheduleLoading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
            )}

            {!scheduleLoading && schedule && (
              <ScheduleCalendar
                sections={schedule?.sections}
                title=""
                semesterStartDate={schedule.semesterStartDate}
                semesterEndDate={schedule.semesterEndDate}
                height={1000}
              />
            )}

            {!scheduleLoading && (!schedule || schedule?.sections.length === 0) && (
              <Alert severity="info">
                No schedule found for semester {semesterId}. Generate one using the form on the left.
              </Alert>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AdminDashboard;

