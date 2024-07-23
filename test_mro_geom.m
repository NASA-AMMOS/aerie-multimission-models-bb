path_kernels = fullfile(pwd,'spice','kernels');
metakernel_fname = 'latest_meta_kernel.tm';

sc_name = 'MRO';
sc_id = '-74';
target = 'MARS';
abcorr = 'LT+S';

fprintf('--------------------------------------------------\n');
fprintf('Loading SPICE Kernels:\n');
fprintf('--------------------------------------------------\n');

fprintf('Loading %s Meta Kernel:\n', sc_name);
fprintf('  %s\n',fullfile(path_kernels, metakernel_fname));
cspice_furnsh( fullfile(path_kernels, metakernel_fname) );



% Input Time
test_time_utc = '2024-01-02 00:00:00 UTC';
test_time_et = cspice_str2et( test_time_utc );

% Get State
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
fprintf('%s State at %s \n', sc_name, test_time_utc);
fprintf('X pos: = %0.5f\n',ptarg(1));
fprintf('Y pos: = %0.5f\n',ptarg(2));
fprintf('Z pos: = %0.5f\n',ptarg(3));

fprintf('\n')

%%%%%%%%%%%%%%%%
% Get Range
%%%%%%%%%%%%%%%%
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
fprintf('%s Range at %s \n', sc_name, test_time_utc);
fprintf('Range: = %0.5f\n',norm(ptarg)) 

%%%%%%%%%%%%%%%%
% Get Speed
%%%%%%%%%%%%%%%%
[starg, lt] = cspice_spkezr(target, test_time_et, 'J2000', abcorr, sc_id);
fprintf('%s Speed at %s \n', sc_name, test_time_utc);
fprintf('Speed: = %0.5f\n',norm(starg(4:6)));

%%%%%%%%%%%%%%%%
% Get Sub-Spacecraft Point and Altitude
%%%%%%%%%%%%%%%%
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
% Get intersection point on the body
[ spoint, trgepc, srfvec, found ] = cspice_sincpt( 'Ellipsoid', target,         ...
                                                   test_time_et, 'IAU_MARS', abcorr, ...
                                                   sc_id, 'J2000',   ptarg );
% Determine the radius of the intersection point relative to the center of
% the body
[radius, lon, lat] = cspice_reclat(spoint);
fprintf('%s Sub-Spacecraft Point at %s \n', sc_name, test_time_utc);
fprintf('Lat: = %0.5f\n',lat*180/pi);
fprintf('Long: = %0.5f\n',lon*180/pi);
fprintf('Radius: = %0.5f\n',radius);

% subtract radius from s/c position relative to center of body 
alt = norm(ptarg) - radius;
                                              
fprintf('%s Altitude at %s \n', sc_name, test_time_utc);
fprintf('Altitude: = %0.5f\n',alt);

%%%%%%%%%%%%%%%%
% Get SEP Angle
%%%%%%%%%%%%%%%%
[starg, lt] = cspice_spkezr(sc_id, test_time_et, 'J2000', abcorr, 'EARTH');
[starg2, lt2] = cspice_spkezr('SUN', test_time_et, 'J2000', abcorr, 'EARTH');
sep_rad = cspice_vsep( starg(1:3), starg2(1:3));
fprintf('%s SEP angle at %s \n', sc_name, test_time_utc);
fprintf('SEP: = %0.5f\n', sep_rad*180/pi);

%%%%%%%%%%%%%%%%
% Get SPE Angle
%%%%%%%%%%%%%%%%
[starg, lt] = cspice_spkezr('EARTH', test_time_et, 'J2000', abcorr, sc_id);
[starg2, lt2] = cspice_spkezr('SUN', test_time_et, 'J2000', abcorr, sc_id);
sep_rad = cspice_vsep( starg(1:3), starg2(1:3));
fprintf('%s SPE angle at %s \n', sc_name, test_time_utc);
fprintf('SPE: = %0.5f\n', sep_rad*180/pi);

%%%%%%%%%%%%%%%%
% Get ESP Angle
%%%%%%%%%%%%%%%%
[starg, lt] = cspice_spkezr('EARTH', test_time_et, 'J2000', abcorr, 'SUN');
[starg2, lt2] = cspice_spkezr(sc_id, test_time_et, 'J2000', abcorr, 'SUN');
sep_rad = cspice_vsep( starg(1:3), starg2(1:3));
fprintf('%s ESP angle at %s \n', sc_name, test_time_utc);
fprintf('ESP: = %0.5f\n', sep_rad*180/pi);

%%%%%%%%%%%%%%%%
% Get Earth-Probe-Target Angle
%%%%%%%%%%%%%%%%
[starg, lt] = cspice_spkezr('EARTH', test_time_et, 'J2000', abcorr, sc_id);
[starg2, lt2] = cspice_spkezr(target, test_time_et, 'J2000', abcorr, sc_id);
sep_rad = cspice_vsep( starg(1:3), starg2(1:3));
fprintf('%s Earth-Probe-Target angle at %s \n', sc_name, test_time_utc);
fprintf('Earth-Probe-Target: = %0.5f\n', sep_rad*180/pi);

%%%%%%%%%%%%%%%%
% Get Illumination Angles at Sub-Spacecraft Point
%%%%%%%%%%%%%%%%
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
% Get intersection point on the body
[ spoint, trgepc, srfvec, found ] = cspice_sincpt( 'Ellipsoid', target,         ...
                                                   test_time_et, 'IAU_MARS', abcorr, ...
                                                   sc_id, 'J2000',   ptarg );
                                               
[trgepc, srfvec, phase, incdnc, emissn] = cspice_ilumin('Ellipsoid', target, test_time_et, 'IAU_MARS', abcorr, sc_id, spoint);
fprintf('%s Sub-Spacecraft Ilumination Angles at %s \n', sc_name, test_time_utc);
fprintf('Phase: = %0.5f\n', phase*180/pi);
fprintf('Incidence = %0.5f\n', incdnc*180/pi);
fprintf('Emission: = %0.5f\n', emissn*180/pi);

%%%%%%%%%%%%%%%%
% Get Orbital Elements
%%%%%%%%%%%%%%%%

% Get gravitational parameter for Mars
mu = cspice_bodvcd(499, 'GM', 1);

% Get state of s/c with respect to central body (target) 
[starg, lt] = cspice_spkezr(target, test_time_et, 'J2000', abcorr, sc_id);

[rotate] = cspice_pxform( 'J2000', 'IAU_MARS', test_time_et );

pos_mars = rotate * starg(1:3);
v_mars = rotate * starg(4:6);

% The elements are, in order:
% 
%   RP      Perifocal distance.
%   ECC     Eccentricity.
%   INC     Inclination.
%   LNODE   Longitude of the ascending node.
%   ARGP    Argument of periapsis.
%   M0      Mean anomaly at epoch.
%   T0      Epoch.
%   MU      Gravitational parameter.
%[elts] = cspice_oscelt( starg, test_time_et, mu );
[elts] = cspice_oscelt( [pos_mars; v_mars], test_time_et, mu );

fprintf('%s Orbital Elements at %s \n', sc_name, test_time_utc);

semi_major_axis = elts(1) / (1-elts(2));
period = 2*pi*sqrt(semi_major_axis^3 / elts(8));
    
fprintf('Orbital Period = %0.5f\n', period);
fprintf('Inclination: = %0.5f\n', elts(3)*180/pi);

%%%%%%%%%%%%%%%%
% Get Beta Angle
%%%%%%%%%%%%%%%%
% Beta angle is the angle between the orbiting body to Sun vector and the 
% orbital plane of the spacecraft
[starg, lt] = cspice_spkezr(target, test_time_et, 'J2000', abcorr, sc_id);

% Compute the normal vector for the orbital plane
orb_normal = cross(starg(1:3),starg(4:6));

% Compute the vector projection of the orbiting body to vector onto
% the orbital plane
[ptarg, lt] = cspice_spkpos('SUN', test_time_et, 'J2000', abcorr, target);
v_proj = ptarg - (dot(ptarg,orb_normal)/norm(orb_normal)^2)*orb_normal;
beta_angle = cspice_vsep( ptarg, v_proj);
fprintf('%s Beta Angle at %s \n', sc_name, test_time_utc);
fprintf('Beta Angle: = %0.5f\n', beta_angle*180/pi);


%%%%%%%%%%%%%%%%
% Get Body Half Angle Size
%%%%%%%%%%%%%%%%
targ_radii = cspice_bodvcd(499, 'RADII', 3);
avg_eq_radii = (targ_radii(1) + targ_radii(2))/2;
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
half_ang = asin(avg_eq_radii/norm(ptarg))*180/pi;
fprintf('%s Body Half Angle Size at %s \n', sc_name, test_time_utc);
fprintf('Half Angle Size: = %0.5f\n', half_ang);

%%%%%%%%%%%%%%%%
% Get RA/DEC
%%%%%%%%%%%%%%%%
% Get position of spacecraft relative to Earth
[ptarg, lt] = cspice_spkpos(sc_id, test_time_et, 'J2000', abcorr, 'Earth');

% Convert to ra/dec coordinates (note the range for ra is [-pi, pi] in the
% geometry model
[range, ra, dec] = cspice_recrad(ptarg);
if (ra > pi)
    ra = ra - 2*pi; 
end

fprintf('%s RA/DEC at %s \n', sc_name, test_time_utc);
fprintf('RA: = %0.5f\n', ra*180/pi);
fprintf('DEC: = %0.5f\n', dec*180/pi);

%%%%%%%%%%%%%%%%
% Get LST
%%%%%%%%%%%%%%%%
[ptarg, lt] = cspice_spkpos(target, test_time_et, 'J2000', abcorr, sc_id);
% Get intersection point on the body
[ spoint, trgepc, srfvec, found ] = cspice_sincpt( 'Ellipsoid', target,         ...
                                                   test_time_et, 'IAU_MARS', abcorr, ...
                                                   sc_id, 'J2000',   ptarg );
% Determine the radius of the intersection point relative to the center of
% the body
[radius, lon, lat] = cspice_reclat(spoint);
[hr, mn, sc, time, ampm] = cspice_et2lst( test_time_et, 499, lon, 'PLANETOCENTRIC' ); 
decimal_hrs = hr + mn/60.0 + sc/3600.0; 
fprintf('%s LST at %s \n', sc_name, test_time_utc);
fprintf('LST: = %0.5f\n', decimal_hrs);


cspice_kclear;



 