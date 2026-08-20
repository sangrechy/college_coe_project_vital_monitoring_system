import time

class HistoryStore:
    def __init__(self):
        self.vitals = []
        self.ecg = []
        self.start_time = time.time()

    def add_record(self, vital_record, ecg_record):
        self.vitals.append(vital_record)
        self.ecg.append(ecg_record)

    def get_vitals(self, minutes):
        cutoff_ms = (time.time() - (minutes * 60)) * 1000
        return [v for v in self.vitals if v["time"] >= cutoff_ms]

    def get_ecg(self, minutes):
        cutoff_ms = (time.time() - (minutes * 60)) * 1000
        return [e for e in self.ecg if e["time"] >= cutoff_ms]

    def clear(self):
        self.vitals = []
        self.ecg = []
        
# Global singleton instance to hold in-memory data
store = HistoryStore()
