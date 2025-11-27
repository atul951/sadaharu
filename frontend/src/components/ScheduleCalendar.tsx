import { useMemo, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { Box, Paper, Typography, Chip, Modal } from '@mui/material';
import type { CalendarEvent, Section } from '../types';
import { sectionsToCalendarEvents } from '../utils/calendarUtils';
import TablePopup from './TablePopup';

interface ScheduleCalendarProps {
  sections: Section[];
  semesterStartDate: Date | string,
  semesterEndDate: Date | string,
  title?: string;
  initialView?: 'timeGridWeek' | 'dayGridMonth';
  height?: string | number;
  onEventClick?: (sectionId: number) => void;
}

const ScheduleCalendar = ({
  sections,
  semesterStartDate,
  semesterEndDate,
  title = 'Schedule',
  initialView = 'timeGridWeek',
  height = 'auto',
  onEventClick,
}: ScheduleCalendarProps) => {

  const [openModel, setOpenModel] = useState<boolean>(false);
  const [eventData, setEventData] = useState<CalendarEvent[]>([]);
  
  const events = useMemo(() => {
    const k = sectionsToCalendarEvents(sections, new Date(semesterStartDate));
    return k;
  }, [sections]);

  const handleEventClick = (info: any) => {
    if (onEventClick) {
      const sectionId = info.event.extendedProps.sectionId;
      onEventClick(sectionId);
    }
  };

  const handleCloseModel = () => {
    setOpenModel(false);
  }

  const style = {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 800,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
  };

  const handleModelOpen = (events: CalendarEvent[]) => {
    setEventData(events);
    setOpenModel(true);
  }

  return (
    <Paper elevation={2} sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">{title}</Typography>
        <Chip label={`${sections.length} Sections`} color="primary" size="small" />
      </Box>

      <FullCalendar
        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
        initialView={initialView}
        headerToolbar={{
          left: '',
          center: 'customTitle',
          right: '',

        }}
        customButtons={{
          customTitle: {
            text: `${semesterStartDate} - ${semesterEndDate}`
          }
        }}
        dayHeaderFormat={{weekday: 'short'}}
        events={events}
        eventClick={handleEventClick}
        slotMinTime="09:00:00"
        slotMaxTime="17:00:00"
        allDaySlot={false}
        height={height}
        weekends={false}
        slotDuration="01:00:00"
        initialDate={semesterStartDate}
        validRange={{
          start: semesterStartDate,
          end: semesterEndDate
        }}
        slotLabelInterval="01:00:00"
        expandRows={true}
        eventContent={(eventInfo) => {
          const { event } = eventInfo;
          const props = event.extendedProps;

          return (
            <Box
              sx={{
                p: 0.5,
                overflow: 'hidden',
                fontSize: '0.75rem',
              }}
              display={'flex'}
              flexDirection={'column'}
            >
              <>
                {
                  props.events.slice(0, 2).map((eve: CalendarEvent) => (
                    <Chip key={eve.id} sx={{height:'18px', mb:'2px'}} label={eve?.title}/>
                  ))
                }
              </>
              <Chip
                key={'random_id'}
                sx={{height:'18px', marginTop:'40px'}}
                color='primary'
                label={`+${props.events.length - 2} more`} 
                onClick={() => handleModelOpen(props.events)}
              />
            </Box>
          );
        }}
      />
      <Modal
        open={openModel}
        onClose={handleCloseModel}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <Box sx={style} alignContent={'center'}>
          <Typography variant="h6" gutterBottom textAlign={'center'}>
            {eventData?.[0]?.timeSlot}
          </Typography>
          <TablePopup event={eventData} />
          <Typography gutterBottom textAlign={'right'} mt={'10px'}>
            {`Total number of slots : ${eventData.length}`}
          </Typography>
        </Box>
      </Modal>
    </Paper>
  );
};

export default ScheduleCalendar;

