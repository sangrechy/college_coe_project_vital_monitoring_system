"""
Pydantic models for ESP32 health data packets.
"""

from typing import Optional
from pydantic import BaseModel


class RawData(BaseModel):
    ecg: Optional[int]   = None
    temp: Optional[float] = None
    ir: Optional[int]    = None
    red: Optional[int]   = None
    ax: Optional[int]    = None
    ay: Optional[int]    = None
    az: Optional[int]    = None


class Features(BaseModel):
    ecg_filt: Optional[int]   = None
    rr:       Optional[float] = None
    hr:       Optional[float] = None
    hrv:      Optional[float] = None
    motion:   Optional[float] = None


class AIOutput(BaseModel):
    ecg_class:    Optional[int] = None
    stress_class: Optional[int] = None
    status:       Optional[int] = None


class HealthPacket(BaseModel):
    raw:      Optional[RawData]  = None
    features: Optional[Features] = None
    ai:       Optional[AIOutput] = None
